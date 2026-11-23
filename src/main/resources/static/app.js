const SESSION_KEY = "shoplite-user";
const CURRENCY = "RM ";

const PRODUCT_CATEGORIES = {
  "Cotton T-Shirt": "Apparel",
  "Blue Jeans": "Apparel",
  "Denim Jacket": "Apparel",
  "Sweater": "Apparel",
  "Cargo Pants": "Apparel",
  "Polo Shirt": "Apparel",
  "Leather Belt": "Apparel",
  "Mechanical Keyboard": "Electronics",
  "Wireless Mouse": "Electronics",
  "USB-C Hub": "Electronics",
  "Cooling Fan Pad": "Electronics",
  "Noise Cancelling Headphones": "Electronics",
  "Bluetooth Speaker": "Electronics",
  "Smart Watch": "Electronics",
  "Wireless Earbuds": "Electronics",
  "Webcam": "Electronics",
  "Running Shoes": "Essentials",
  "Canvas Backpack": "Essentials",
  "Water Bottle": "Essentials",
  "Travel Mug": "Essentials",
  "Umbrella": "Essentials",
  "Notebook Set": "Essentials",
};

const state = {
  user: JSON.parse(localStorage.getItem(SESSION_KEY) || "null"),
  sessionId: 0,
  expandedOrders: new Set(),
  orderDetails: new Map(),
  cartCount: 0,
  botConversationId: null,
  botMessages: [],
  abandonTimer: null,
};

let lastOrderBanner = null;
let adminEditId = null;
let retentionUsers = [];

const app = document.getElementById("app");

function currentUserId() {
  return state.user ? state.user.id : null;
}

function isAdmin() {
  return Boolean(state.user && state.user.role === "ADMIN");
}

function isCustomer() {
  return Boolean(state.user && state.user.role === "CUSTOMER");
}

async function api(path, options = {}) {
  const config = { ...options };
  const headers = {};
  if (state.user) {
    headers["X-User-Id"] = String(state.user.id);
  }
  if (options.body) {
    headers["Content-Type"] = "application/json";
  }
  config.headers = headers;
  let response;
  try {
    response = await fetch(path, config);
  } catch (error) {
    throw { status: 0, message: "Cannot reach the server. Is the backend running?" };
  }
  if (response.status === 204) {
    return null;
  }
  const body = await response.json().catch(() => null);
  if (!response.ok) {
    throw {
      status: response.status,
      message: (body && body.message) || `Request failed (${response.status}).`,
    };
  }
  return body;
}

function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, (character) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#39;",
  })[character]);
}

function money(value) {
  const amount = Number(value).toLocaleString("en-US", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
  return `${CURRENCY}${amount}`;
}

function formatDate(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "";
  }
  return date.toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" });
}

function showToast(message, type = "success") {
  const container = document.getElementById("toasts");
  const toast = document.createElement("div");
  toast.className = `toast toast-${type}`;
  toast.textContent = message;
  container.appendChild(toast);
  setTimeout(() => toast.remove(), 3500);
}

function updateHeader(route) {
  document.querySelectorAll(".nav a").forEach((link) => {
    link.classList.toggle("active", link.dataset.tab === route);
  });
  const loggedIn = Boolean(state.user);
  const admin = isAdmin();
  document.querySelectorAll(".customer-tab").forEach((link) => link.classList.toggle("hidden", !loggedIn || admin));
  document.getElementById("admin-tab").classList.toggle("hidden", !admin);
  document.getElementById("dashboard-tab").classList.toggle("hidden", !admin);
  const area = document.getElementById("user-area");
  if (state.user) {
    area.innerHTML = `
      <span class="user-chip">${escapeHtml(state.user.name)}
        <span class="role-badge">${escapeHtml(state.user.role)}</span>
      </span>
      <button class="button ghost" data-action="logout">Log out</button>`;
  } else {
    area.innerHTML = "";
  }
}

async function refreshCartBadge() {
  const badge = document.getElementById("cart-badge");
  const cartBtn = document.getElementById("floating-cart");
  const cartCountBadge = document.getElementById("floating-cart-count");
  const show = isCustomer();
  if (cartBtn) {
    cartBtn.classList.toggle("hidden", !show);
  }
  if (!show) {
    state.cartCount = 0;
    badge.classList.add("hidden");
    if (cartCountBadge) {
      cartCountBadge.classList.add("hidden");
    }
    return;
  }
  const session = state.sessionId;
  try {
    const cart = await api(`/api/users/${state.user.id}/cart`);
    if (session !== state.sessionId) {
      return;
    }
    const count = cart.items.reduce((sum, item) => sum + item.quantity, 0);
    state.cartCount = count;
    badge.textContent = String(count);
    badge.classList.toggle("hidden", count === 0);
    if (cartCountBadge) {
      cartCountBadge.textContent = String(count);
      cartCountBadge.classList.toggle("hidden", count === 0);
    }
  } catch (error) {
    if (session !== state.sessionId) {
      return;
    }
    state.cartCount = 0;
    badge.classList.add("hidden");
    if (cartCountBadge) {
      cartCountBadge.classList.add("hidden");
    }
  }
}

const routes = ["products", "cart", "orders", "admin", "dashboard", "login"];

async function render() {
  const route = (location.hash || "#products").replace("#", "");
  const current = routes.includes(route) ? route : "products";
  updateHeader(current);
  if (!state.user && current !== "login") {
    location.hash = "#login";
    return;
  }
  if (state.user && current === "login") {
    location.hash = isAdmin() ? "#dashboard" : "#products";
    return;
  }
  if (isAdmin() && ["products", "cart", "orders"].includes(current)) {
    location.hash = "#dashboard";
    return;
  }
  app.innerHTML = '<p class="loading">Loading...</p>';
  try {
    if (current === "login") {
      renderLogin();
    } else if (current === "cart") {
      await renderCart();
    } else if (current === "orders") {
      await renderOrders();
    } else if (current === "admin") {
      await renderAdmin();
    } else if (current === "dashboard") {
      await renderDashboard();
    } else {
      await renderProducts();
    }
  } catch (error) {
    app.innerHTML = `
      <div class="empty-state error-state">
        <h2>Something went wrong</h2>
        <p>${escapeHtml(error.message || "Unexpected error.")}</p>
      </div>`;
  }
}

function renderLogin() {
  app.innerHTML = `
    <div class="login-wrap">
      <form class="login-card" data-form="login">
        <h1>Welcome to ShopLite</h1>
        <p class="muted">Sign in with your account email.</p>
        <label>Email
          <input name="email" type="email" required placeholder="you@example.com" />
        </label>
        <button class="button primary full-width">Sign in</button>
        <p class="login-hint">Demo accounts: alice@example.com (customer), admin@shop.test (admin)</p>
      </form>
    </div>`;
}

async function handleLogin(form) {
  const email = form.email.value.trim();
  const user = await api("/api/auth/login", {
    method: "POST",
    body: JSON.stringify({ email }),
  });
  state.user = user;
  state.sessionId += 1;
  localStorage.setItem(SESSION_KEY, JSON.stringify(user));
  showToast(`Welcome, ${user.name}.`);
  if (isAdmin()) {
    location.hash = "#dashboard";
  } else {
    location.hash = "#products";
    showBubble();
    checkEngagement();
  }
}

function logout() {
  state.user = null;
  state.sessionId += 1;
  localStorage.removeItem(SESSION_KEY);
  state.expandedOrders.clear();
  state.orderDetails.clear();
  state.botMessages = [];
  state.botConversationId = null;
  if (state.abandonTimer) {
    clearTimeout(state.abandonTimer);
    state.abandonTimer = null;
  }
  hideBot();
  showToast("Logged out.");
  location.hash = "#login";
  refreshCartBadge();
  if (location.hash === "#login") {
    render();
  }
}

async function renderProducts() {
  const products = await api("/api/products");
  if (!products.length) {
    app.innerHTML = `
      <section>
        <h1>Products</h1>
        <div class="empty-state"><h2>No products</h2><p>The catalog is empty.</p></div>
      </section>`;
    return;
  }
  const sections = groupProductsByCategory(products);
  app.innerHTML = `
    <section>
      <h1>Products</h1>
      ${sections.map(renderCategorySection).join("")}
    </section>`;
}

function groupProductsByCategory(products) {
  const order = ["Apparel", "Electronics", "Essentials"];
  const groups = new Map();
  products.forEach((product) => {
    const category = PRODUCT_CATEGORIES[product.name] || "Other";
    if (!groups.has(category)) {
      groups.set(category, []);
    }
    groups.get(category).push(product);
  });
  return [...groups.keys()]
    .sort((a, b) => {
      const ia = order.indexOf(a);
      const ib = order.indexOf(b);
      if (ia >= 0 && ib >= 0) {
        return ia - ib;
      }
      if (ia >= 0) {
        return -1;
      }
      if (ib >= 0) {
        return 1;
      }
      return a.localeCompare(b);
    })
    .map((category) => ({ category, products: groups.get(category) }));
}

function renderCategorySection(section) {
  return `
    <div class="catalog-section">
      <h2>${escapeHtml(section.category)}</h2>
      <div class="product-grid">${section.products.map(productCard).join("")}</div>
    </div>`;
}

function productCard(product) {
  const outOfStock = product.availableQuantity === 0;
  const options = Array.isArray(product.variantOptions) ? product.variantOptions : [];
  const variantPicker = product.variantName && options.length
    ? `<label class="variant-picker"><span>${escapeHtml(product.variantName)}</span>
         <select class="variant-select" data-product-id="${product.id}">
           ${options.map((option) => `<option value="${escapeHtml(option)}">${escapeHtml(option)}</option>`).join("")}
         </select>
       </label>`
    : "";
  return `
    <article class="product-card">
      <h3>${escapeHtml(product.name)}</h3>
      <p class="product-description">${escapeHtml(product.description)}</p>
      <div class="product-meta">
        <span class="price">${money(product.price)}</span>
        <span class="chip ${outOfStock ? "chip-danger" : "chip-success"}">${
          outOfStock ? "Out of stock" : `${product.availableQuantity} in stock`
        }</span>
      </div>
      ${variantPicker}
      <button class="button primary" data-action="add-to-cart" data-product-id="${product.id}" ${
        outOfStock ? "disabled" : ""
      }>Add to cart</button>
    </article>`;
}

function emptyCartSection() {
  return `
    <section>
      <h1>Your cart</h1>
      <div class="empty-state">
        <h2>Your cart is empty</h2>
        <p>Browse the catalog and add something you like.</p>
        <a class="button primary" href="#products">Go to products</a>
      </div>
    </section>`;
}

async function renderCart() {
  let cart;
  try {
    cart = await api(`/api/users/${state.user.id}/cart`);
  } catch (error) {
    if (error.status === 400) {
      app.innerHTML = emptyCartSection();
      return;
    }
    throw error;
  }
  if (!cart.items.length) {
    app.innerHTML = emptyCartSection();
    return;
  }
  const hasDiscontinued = cart.items.some((item) => item.discontinued);
  app.innerHTML = `
    <section>
      <h1>Your cart</h1>
      <div class="cart-panel">
        ${cart.items.map(cartRow).join("")}
        <div class="cart-total">
          <span>Total</span>
          <strong>${money(cart.totalAmount)}</strong>
        </div>
        ${
          hasDiscontinued
            ? '<div class="banner warning-banner">This cart contains discontinued items. Remove them to enable checkout.</div>'
            : ""
        }
        <div class="cart-actions">
          <a class="button ghost" href="#products">Continue shopping</a>
          ${
            hasDiscontinued
              ? ""
              : '<button class="button primary" data-action="checkout">Checkout</button>'
          }
        </div>
      </div>
    </section>`;
}

function cartRow(item) {
  const variantAttr = escapeHtml(item.variant || "");
  const variantLabel = item.variant ? `<span class="item-variant">${escapeHtml(item.variant)}</span>` : "";
  if (item.discontinued) {
    return `
      <div class="cart-row discontinued-row">
        <div class="cart-item-name">
          <strong>${escapeHtml(item.productName)}</strong>
          ${variantLabel}
          <span class="chip chip-danger">Discontinued</span>
        </div>
        <span class="line-total">${money(item.lineTotal)}</span>
        <button class="button danger" data-action="remove-item" data-product-id="${item.productId}" data-variant="${variantAttr}">Remove</button>
      </div>`;
  }
  return `
    <div class="cart-row">
      <div class="cart-item-name">
        <strong>${escapeHtml(item.productName)}</strong>
        ${variantLabel}
        <span>${money(item.price)} each</span>
      </div>
      <div class="stepper">
        <button class="stepper-button" data-action="dec-quantity" data-product-id="${item.productId}" data-variant="${variantAttr}" ${
          item.quantity <= 1 ? "disabled" : ""
        } aria-label="Decrease quantity">&minus;</button>
        <span class="quantity">${item.quantity}</span>
        <button class="stepper-button" data-action="inc-quantity" data-product-id="${item.productId}" data-variant="${variantAttr}" aria-label="Increase quantity">+</button>
      </div>
      <span class="line-total">${money(item.lineTotal)}</span>
      <button class="button danger" data-action="remove-item" data-product-id="${item.productId}" data-variant="${variantAttr}">Remove</button>
    </div>`;
}

function takeLastOrderBanner() {
  const banner = lastOrderBanner;
  lastOrderBanner = null;
  return banner;
}

async function renderOrders() {
  const orders = await api(`/api/users/${state.user.id}/orders`);
  const banner = takeLastOrderBanner();
  if (!orders.length) {
    app.innerHTML = `
      <section>
        <h1>Your orders</h1>
        ${banner || ""}
        <div class="empty-state"><h2>No orders yet</h2><p>Items you check out will appear here.</p></div>
      </section>`;
    return;
  }
  app.innerHTML = `
    <section>
      <h1>Your orders</h1>
      ${banner || ""}
      <div class="order-list">${orders.map(orderRow).join("")}</div>
    </section>`;
}

function orderRow(order) {
  const expanded = state.expandedOrders.has(order.orderId);
  return `
    <article class="order-card">
      <button class="order-header" data-action="toggle-order" data-order-id="${order.orderId}">
        <span class="order-id">Order #${order.orderId}</span>
        <span class="chip chip-info">${escapeHtml(order.status)}</span>
        <span class="order-date">${formatDate(order.createdAt)}</span>
        <span class="order-total">${money(order.totalAmount)}</span>
        <span class="chevron">${expanded ? "&#9652;" : "&#9662;"}</span>
      </button>
      <div class="order-detail" ${expanded ? "" : "hidden"}>${
        expanded ? renderOrderDetail(order.orderId) : ""
      }</div>
    </article>`;
}

function renderOrderDetail(orderId) {
  const detail = state.orderDetails.get(orderId);
  if (!detail) {
    return '<p class="loading">Loading items...</p>';
  }
  return `
    <table class="detail-table">
      <thead>
        <tr><th>Product</th><th>Unit price</th><th>Qty</th><th>Line total</th></tr>
      </thead>
      <tbody>
        ${detail.items
          .map(
            (item) => `
          <tr>
            <td>${escapeHtml(item.productName)}</td>
            <td>${money(item.price)}</td>
            <td>${item.quantity}</td>
            <td>${money(item.lineTotal)}</td>
          </tr>`
          )
          .join("")}
      </tbody>
    </table>`;
}

async function toggleOrder(orderId) {
  if (state.expandedOrders.has(orderId)) {
    state.expandedOrders.delete(orderId);
  } else {
    state.expandedOrders.add(orderId);
    if (!state.orderDetails.has(orderId)) {
      const detail = await api(`/api/orders/${orderId}`);
      state.orderDetails.set(orderId, detail);
    }
  }
  await renderOrders();
}

async function renderAdmin() {
  if (!isAdmin()) {
    location.hash = "#products";
    return;
  }
  const products = await api("/api/products/all");
  app.innerHTML = `
    <section>
      <h1>Product management</h1>
      <div class="admin-panel">
        <form class="admin-create" data-form="create-product">
          <h2>New product</h2>
          <div class="form-grid">
            <label>Name<input name="name" required maxlength="150" /></label>
            <label>Price<input name="price" type="number" step="0.01" min="0" required /></label>
            <label>Description<input name="description" required maxlength="500" /></label>
            <label>Stock<input name="availableQuantity" type="number" min="0" required /></label>
            <label>Variant name<input name="variantName" maxlength="50" placeholder="e.g. Size" /></label>
            <label>Variant options<input name="variantOptions" maxlength="200" placeholder="e.g. M,L" /></label>
          </div>
          <div class="edit-actions"><button class="button primary">Create product</button></div>
        </form>
        <div class="table-scroll">
          <table class="admin-table">
            <thead>
              <tr><th>ID</th><th>Name</th><th>Description</th><th>Price</th><th>Stock</th><th>Status</th><th></th></tr>
            </thead>
            <tbody>${
              products.length
                ? products.map(adminRow).join("")
                : '<tr><td colspan="7" class="loading">No products yet.</td></tr>'
            }</tbody>
          </table>
        </div>
      </div>
    </section>`;
}

function adminRow(product) {
  if (adminEditId === product.id) {
    return adminEditRow(product);
  }
  const discontinued = product.status === "DISCONTINUED";
  return `
    <tr class="${discontinued ? "row-discontinued" : ""}">
      <td>${product.id}</td>
      <td>${escapeHtml(product.name)}</td>
      <td class="cell-description">${escapeHtml(product.description)}</td>
      <td>${money(product.price)}</td>
      <td>${product.availableQuantity}</td>
      <td><span class="chip ${discontinued ? "chip-danger" : "chip-success"}">${product.status}</span></td>
      <td class="cell-actions">
        <button class="button ghost" data-action="edit-product" data-product-id="${product.id}">Edit</button>
        ${
          discontinued
            ? ""
            : `<button class="button danger" data-action="delete-product" data-product-id="${product.id}">Delete</button>`
        }
      </td>
    </tr>`;
}

function adminEditRow(product) {
  return `
    <tr>
      <td>${product.id}</td>
      <td colspan="6">
        <form class="admin-edit" data-form="update-product" data-product-id="${product.id}">
          <div class="form-grid">
            <label>Name<input name="name" value="${escapeHtml(product.name)}" required maxlength="150" /></label>
            <label>Price<input name="price" type="number" step="0.01" min="0" value="${product.price}" required /></label>
            <label>Description<input name="description" value="${escapeHtml(product.description)}" required maxlength="500" /></label>
            <label>Stock<input name="availableQuantity" type="number" min="0" value="${product.availableQuantity}" required /></label>
            <label>Variant name<input name="variantName" maxlength="50" value="${escapeHtml(product.variantName || "")}" /></label>
            <label>Variant options<input name="variantOptions" maxlength="200" value="${escapeHtml((product.variantOptions || []).join(","))}" /></label>
          </div>
          <div class="edit-actions">
            <button class="button primary">Save</button>
            <button type="button" class="button ghost" data-action="cancel-edit">Cancel</button>
          </div>
        </form>
      </td>
    </tr>`;
}

async function renderDashboard() {
  if (!isAdmin()) {
    location.hash = "#products";
    return;
  }
  const summary = await api("/api/analytics/summary");
  retentionUsers = await api("/api/analytics/users");
  app.innerHTML = `
    <section class="dashboard">
      <h1>Monitoring Dashboard</h1>
      <div class="kpi-grid">
        <div class="kpi-card"><span class="kpi-label">Total Users</span><span class="kpi-value">${summary.totalUsers}</span></div>
        <div class="kpi-card"><span class="kpi-label">At-Risk Users</span><span class="kpi-value">${summary.atRiskUsers}</span></div>
        <div class="kpi-card"><span class="kpi-label">Churn Rate</span><span class="kpi-value">${summary.churnRatePercent}%</span></div>
        <div class="kpi-card"><span class="kpi-label">Avg Risk Score</span><span class="kpi-value">${summary.avgRiskScore}/100</span></div>
        <div class="kpi-card"><span class="kpi-label">Recoverable Revenue</span><span class="kpi-value">${money(summary.recoverableRevenue)}</span></div>
        <div class="kpi-card"><span class="kpi-label">Actual Revenue</span><span class="kpi-value">${money(summary.totalRevenue)}</span></div>
      </div>
      <div class="chart-card">
        <h2>Abandoned carts per day (last 30 days)</h2>
        <div class="bar-chart">${churnChart(summary.churnTrend)}</div>
      </div>
      <div class="chart-card">
        <div class="retention-head">
          <h2>Customer Retention</h2>
          <input id="retention-search" type="text" placeholder="Search customers..." />
        </div>
        <div class="table-scroll">
          <table class="admin-table retention-table">
            <thead>
              <tr><th>Customer</th><th>Risk</th><th>Orders</th><th>Total Spend</th><th>Abandoned</th><th>Recoverable</th><th>Inactive</th><th>Why at risk</th></tr>
            </thead>
            <tbody id="retention-tbody"></tbody>
          </table>
        </div>
      </div>
    </section>`;
  renderRetentionTable("");
}

function churnChart(points) {
  const max = Math.max(1, ...points.map((point) => point.abandonedCarts));
  return points
    .map((point) => {
      const height = Math.max(4, Math.round((point.abandonedCarts / max) * 80));
      return `<div class="bar-col" title="${point.date} — ${point.abandonedCarts} abandoned"><div class="bar-fill" style="height:${height}px"></div></div>`;
    })
    .join("");
}

function retentionRow(user) {
  const riskLevel = user.riskScore >= 70 ? "risk-high" : user.riskScore >= 40 ? "risk-med" : "risk-low";
  return `
    <tr>
      <td><div class="retention-user"><strong>${escapeHtml(user.name)}</strong><span class="muted">${escapeHtml(user.email)}</span></div></td>
      <td><span class="risk-pill ${riskLevel}">${user.riskScore}</span></td>
      <td>${user.orderCount}</td>
      <td>${money(user.totalSpend)}</td>
      <td>${user.abandonedCarts}</td>
      <td>${money(user.recoverableValue)}</td>
      <td>${user.daysInactive > 0 ? user.daysInactive + "d" : "&mdash;"}</td>
      <td class="insights-cell">${
        user.insights.length
          ? user.insights.map((insight) => `<span class="insight-chip">${escapeHtml(insight)}</span>`).join("")
          : '<span class="muted">&mdash;</span>'
      }</td>
    </tr>`;
}

function renderRetentionTable(filter) {
  const tbody = document.getElementById("retention-tbody");
  if (!tbody) {
    return;
  }
  const query = (filter || "").toLowerCase();
  const rows = retentionUsers.filter(
    (user) => user.name.toLowerCase().includes(query) || (user.email && user.email.toLowerCase().includes(query))
  );
  tbody.innerHTML = rows.length
    ? rows.map(retentionRow).join("")
    : '<tr><td colspan="8" class="loading">No customers match.</td></tr>';
}

async function addToCart(productId, variant) {
  await api(`/api/users/${state.user.id}/cart/items`, {
    method: "POST",
    body: JSON.stringify({ productId, quantity: 1, variant: variant || null }),
  });
  showToast("Added to cart.");
  await refreshCartBadge();
  scheduleAbandonmentCheck();
  await render();
}

async function changeQuantity(productId, variant, delta) {
  let cart = null;
  try {
    cart = await api(`/api/users/${state.user.id}/cart`);
  } catch (error) {
    return;
  }
  const item = cart.items.find(
    (entry) => entry.productId === productId && (entry.variant || "") === (variant || "")
  );
  if (!item || item.discontinued) {
    return;
  }
  const nextQuantity = item.quantity + delta;
  if (nextQuantity < 1) {
    return;
  }
  await api(`/api/users/${state.user.id}/cart/items/${productId}?variant=${encodeURIComponent(variant || "")}`, {
    method: "PATCH",
    body: JSON.stringify({ quantity: nextQuantity }),
  });
  await refreshCartBadge();
  await render();
}

async function removeItem(productId, variant) {
  await api(`/api/users/${state.user.id}/cart/items/${productId}?variant=${encodeURIComponent(variant || "")}`, {
    method: "DELETE",
  });
  showToast("Item removed.");
  await refreshCartBadge();
  await render();
}

async function checkout() {
  const order = await api(`/api/users/${state.user.id}/cart/checkout`, { method: "POST" });
  lastOrderBanner = `<div class="banner success-banner">Order #${order.orderId} placed successfully. Total ${money(
    order.totalAmount
  )}.</div>`;
  showToast(`Order #${order.orderId} placed.`);
  state.expandedOrders.clear();
  await refreshCartBadge();
  if (location.hash === "#orders") {
    await render();
  } else {
    location.hash = "#orders";
  }
}

async function handleCreateProduct(form) {
  const data = new FormData(form);
  await api("/api/products", {
    method: "POST",
    body: JSON.stringify({
      name: data.get("name"),
      description: data.get("description"),
      price: Number(data.get("price")),
      availableQuantity: Number(data.get("availableQuantity")),
      variantName: data.get("variantName") || null,
      variantOptions: data.get("variantOptions") || null,
    }),
  });
  showToast("Product created.");
  form.reset();
  await renderAdmin();
}

async function handleUpdateProduct(form) {
  const productId = Number(form.dataset.productId);
  const data = new FormData(form);
  await api(`/api/products/${productId}`, {
    method: "PATCH",
    body: JSON.stringify({
      name: data.get("name"),
      description: data.get("description"),
      price: Number(data.get("price")),
      availableQuantity: Number(data.get("availableQuantity")),
      variantName: data.get("variantName") || null,
      variantOptions: data.get("variantOptions") || null,
    }),
  });
  adminEditId = null;
  showToast("Product updated.");
  await renderAdmin();
}

function scheduleAbandonmentCheck() {
  if (state.abandonTimer) {
    clearTimeout(state.abandonTimer);
  }
  state.abandonTimer = setTimeout(() => checkEngagement(), 15 * 1000);
}

async function checkEngagement() {
  if (!isCustomer()) {
    return;
  }
  const session = state.sessionId;
  try {
    const engagement = await api(`/api/bot/engagement/${state.user.id}`);
    if (session !== state.sessionId) {
      return;
    }
    if (engagement.shouldEngage) {
      state.botConversationId = engagement.conversationId;
      showBot(engagement.opener || "Hi! I noticed you left some items in your cart. Can I help?");
    }
  } catch (error) {
    /* engagement is best-effort */
  }
}

function showBot(opener) {
  state.botMessages.push({ role: "BOT", content: opener });
  renderBotMessages();
  document.getElementById("bot-widget").classList.remove("hidden");
  openBot();
}

function showBubble() {
  if (!isCustomer()) {
    return;
  }
  document.getElementById("bot-widget").classList.remove("hidden");
  document.getElementById("bot-bubble").classList.remove("hidden");
}

function openBot() {
  if (!isCustomer()) {
    return;
  }
  document.getElementById("bot-panel").classList.remove("hidden");
  document.getElementById("bot-bubble").classList.add("hidden");
  document.getElementById("bot-input").focus();
}

function closeBot() {
  document.getElementById("bot-panel").classList.add("hidden");
  document.getElementById("bot-bubble").classList.remove("hidden");
}

function hideBot() {
  document.getElementById("bot-widget").classList.add("hidden");
  document.getElementById("bot-panel").classList.add("hidden");
  document.getElementById("bot-bubble").classList.remove("hidden");
}

function renderBotMessages() {
  const box = document.getElementById("bot-messages");
  box.innerHTML = state.botMessages
    .map((message) => {
      const cls = message.role === "BOT" ? "msg msg-bot" : "msg msg-user";
      let html = `<div class="${cls}">${escapeHtml(message.content)}</div>`;
      if (message.role === "BOT" && message.showCheckout) {
        html += `
          <div class="bot-checkout-footer">
            <p>If you have any other questions, just ask — or click below to check out.</p>
            <a class="button primary bot-checkout-btn" href="#cart">Proceed to checkout</a>
          </div>`;
      }
      return html;
    })
    .join("");
  box.scrollTop = box.scrollHeight;
}

async function sendBotMessage(text) {
  if (!isCustomer()) {
    return;
  }
  const message = text.trim();
  if (!message) {
    return;
  }
  const session = state.sessionId;
  state.botMessages.push({ role: "USER", content: message });
  renderBotMessages();
  try {
    const response = await api("/api/bot/message", {
      method: "POST",
      body: JSON.stringify({ userId: state.user.id, message }),
    });
    if (session !== state.sessionId) {
      return;
    }
    state.botConversationId = response.conversationId;
    state.botMessages.push({ role: "BOT", content: response.reply, showCheckout: state.cartCount > 0 });
  } catch (error) {
    if (session !== state.sessionId) {
      return;
    }
    state.botMessages.push({ role: "BOT", content: error.message || "Sorry, something went wrong." });
  }
  renderBotMessages();
}

document.addEventListener("click", async (event) => {
  const trigger = event.target.closest("[data-action]");
  if (trigger) {
    if (!trigger.disabled) {
      const action = trigger.dataset.action;
      try {
        if (action === "logout") {
          logout();
        } else if (action === "add-to-cart") {
          const card = trigger.closest(".product-card");
          const select = card ? card.querySelector(".variant-select") : null;
          await addToCart(Number(trigger.dataset.productId), select ? select.value : null);
        } else if (action === "inc-quantity") {
          await changeQuantity(Number(trigger.dataset.productId), trigger.dataset.variant, 1);
        } else if (action === "dec-quantity") {
          await changeQuantity(Number(trigger.dataset.productId), trigger.dataset.variant, -1);
        } else if (action === "remove-item") {
          await removeItem(Number(trigger.dataset.productId), trigger.dataset.variant);
        } else if (action === "checkout") {
          await checkout();
        } else if (action === "toggle-order") {
          await toggleOrder(Number(trigger.dataset.orderId));
        } else if (action === "edit-product") {
          adminEditId = Number(trigger.dataset.productId);
          await renderAdmin();
        } else if (action === "cancel-edit") {
          adminEditId = null;
          await renderAdmin();
        } else if (action === "delete-product") {
          const productId = Number(trigger.dataset.productId);
          if (confirm(`Delete product #${productId}? It will be marked as discontinued.`)) {
            await api(`/api/products/${productId}`, { method: "DELETE" });
            showToast("Product deleted.");
            await renderAdmin();
          }
        }
      } catch (error) {
        showToast(error.message || "Something went wrong.", "error");
      }
    }
    return;
  }

  if (event.target.closest("#floating-cart")) {
    location.hash = "#cart";
    return;
  }
  if (event.target.closest("#bot-bubble")) {
    openBot();
    return;
  }
  if (event.target.closest("#bot-close")) {
    closeBot();
  }
});

document.addEventListener("submit", async (event) => {
  const form = event.target.closest("[data-form]");
  if (!form) {
    return;
  }
  event.preventDefault();
  const kind = form.dataset.form;
  try {
    if (kind === "login") {
      await handleLogin(form);
    } else if (kind === "create-product") {
      await handleCreateProduct(form);
    } else if (kind === "update-product") {
      await handleUpdateProduct(form);
    } else if (kind === "bot") {
      const input = form.querySelector("#bot-input");
      const text = input.value;
      input.value = "";
      await sendBotMessage(text);
    }
  } catch (error) {
    showToast(error.message || "Something went wrong.", "error");
  }
});

document.addEventListener("input", (event) => {
  if (event.target.id === "retention-search") {
    renderRetentionTable(event.target.value);
  }
});

window.addEventListener("hashchange", render);

render();
refreshCartBadge();
if (isCustomer()) {
  showBubble();
}
