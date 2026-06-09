// ==================== UTILIDADES GLOBALES ====================

function getToken() {
    return localStorage.getItem('token');
}

function setLoading(show) {
    const loadingDiv = document.getElementById('loading');
    if (loadingDiv) {
        loadingDiv.style.display = show ? 'flex' : 'none';
    }
}

function mostrarMensaje(mensaje, tipo = 'success') {
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${tipo} alert-custom`;
    alertDiv.innerHTML = `<i class="fas fa-${tipo === 'success' ? 'check-circle' : 'exclamation-circle'}"></i> ${mensaje}`;
    document.body.appendChild(alertDiv);
    setTimeout(() => alertDiv.remove(), 3000);
}

async function authFetch(url, options = {}) {
    const tokenActual = getToken();
    if (!tokenActual) {
        window.location.href = '/';
        throw new Error('No token');
    }
    
    options.headers = {
        ...options.headers,
        'Authorization': `Bearer ${tokenActual}`,
        'Content-Type': 'application/json'
    };
    
    const response = await fetch(url, options);
    
    if (response.status === 401) {
        localStorage.clear();
        window.location.href = '/';
        throw new Error('Sesión expirada');
    }
    
    return response;
}

function toggleModoOscuro() {
    const body = document.body;
    if (body.classList.contains('light-mode')) {
        body.classList.remove('light-mode');
        body.classList.add('dark-mode');
        localStorage.setItem('modo', 'dark');
        const toggleBtn = document.querySelector('.theme-toggle i');
        if (toggleBtn) toggleBtn.className = 'fas fa-sun';
    } else {
        body.classList.remove('dark-mode');
        body.classList.add('light-mode');
        localStorage.setItem('modo', 'light');
        const toggleBtn = document.querySelector('.theme-toggle i');
        if (toggleBtn) toggleBtn.className = 'fas fa-moon';
    }
}

function cargarModoOscuro() {
    if (localStorage.getItem('modo') === 'dark') {
        document.body.classList.remove('light-mode');
        document.body.classList.add('dark-mode');
        const toggleBtn = document.querySelector('.theme-toggle i');
        if (toggleBtn) toggleBtn.className = 'fas fa-sun';
    }
}

function formatearFecha(fecha) {
    if (!fecha) return 'N/A';
    const date = new Date(fecha);
    return date.toLocaleDateString('es-MX');
}

function formatearPrecio(precio) {
    if (!precio) return '$0.00';
    return `$${precio.toLocaleString('es-MX')}`;
}

function obtenerBadgeEstado(estado) {
    const estados = {
        'AGENDADA': '<span class="badge-estado badge-agendada">Agendada</span>',
        'EN_PROGRESO': '<span class="badge-estado badge-progreso">En Progreso</span>',
        'COMPLETADA': '<span class="badge-estado badge-completada">Completada</span>',
        'CANCELADA': '<span class="badge-estado badge-cancelada">Cancelada</span>',
        'REAGENDADA': '<span class="badge-estado badge-reagendada">Reagendada</span>'
    };
    return estados[estado] || '<span class="badge-estado badge-agendada">Agendada</span>';
}