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

// ==================== GRÁFICAS DE VISTAS CLÍNICAS (PSICÓLOGO / PSIQUIATRA) ====================
let _grafEstadosClinica = null;
let _grafMesesClinica = null;

function dibujarGraficasClinicas(citas) {
    const canvasEstados = document.getElementById('graficaEstadosClinica');
    const canvasMeses = document.getElementById('graficaMesesClinica');
    if (!canvasEstados || !canvasMeses || typeof Chart === 'undefined') return;
    if (typeof Chart.defaults.font !== 'undefined') Chart.defaults.font.family = "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif";

    citas = Array.isArray(citas) ? citas : [];

    // --- Dona: citas por estado ---
    const ordenEstados = ['AGENDADA', 'REAGENDADA', 'EN_PROGRESO', 'COMPLETADA', 'CANCELADA'];
    const etiquetasEstados = { AGENDADA: 'Agendada', REAGENDADA: 'Reagendada', EN_PROGRESO: 'En progreso', COMPLETADA: 'Completada', CANCELADA: 'Cancelada' };
    const conteoEstados = ordenEstados
        .map(e => ({ estado: e, total: citas.filter(c => c.estado === e).length }))
        .filter(x => x.total > 0);
    const panelEstados = canvasEstados.closest('.grafica-panel');
    _graflimpiaPanel(panelEstados, canvasEstados);
    if (conteoEstados.length === 0) {
        _grafNotaVacia(panelEstados, 'Sin citas asignadas todavía', canvasEstados);
    } else {
        if (_grafEstadosClinica) _grafEstadosClinica.destroy();
        const mapColor = { AGENDADA: '#818cf8', REAGENDADA: '#fcd34d', EN_PROGRESO: '#22d3ee', COMPLETADA: '#34d399', CANCELADA: '#f87171' };
        _grafEstadosClinica = new Chart(canvasEstados.getContext('2d'), {
            type: 'doughnut',
            data: {
                labels: conteoEstados.map(x => etiquetasEstados[x.estado]),
                datasets: [{
                    data: conteoEstados.map(x => x.total),
                    backgroundColor: conteoEstados.map(x => mapColor[x.estado] || '#818cf8'),
                    borderWidth: 0
                }]
            },
            options: {
                responsive: true, maintainAspectRatio: true, cutout: '62%',
                plugins: { legend: { position: 'bottom', labels: { boxWidth: 12, padding: 12, color: '#2d3142', font: { size: 11 } } } }
            }
        });
    }

    // --- Barras: citas por mes ---
    const nombresMes = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];
    const porMes = {};
    citas.forEach(c => {
        if (!c.fecha) return;
        const mes = String(c.fecha).split('-')[1];
        if (mes) porMes[mes] = (porMes[mes] || 0) + 1;
    });
    const mesesOrdenados = Object.keys(porMes).sort();
    const panelMeses = canvasMeses.closest('.grafica-panel');
    _graflimpiaPanel(panelMeses, canvasMeses);
    if (mesesOrdenados.length === 0) {
        _grafNotaVacia(panelMeses, 'Sin citas para graficar todavía', canvasMeses);
    } else {
        if (_grafMesesClinica) _grafMesesClinica.destroy();
        _grafMesesClinica = new Chart(canvasMeses.getContext('2d'), {
            type: 'bar',
            data: {
                labels: mesesOrdenados.map(m => nombresMes[parseInt(m, 10) - 1] || m),
                datasets: [{
                    label: 'Citas',
                    data: mesesOrdenados.map(m => porMes[m]),
                    backgroundColor: 'rgba(102, 126, 234, 0.75)',
                    hoverBackgroundColor: '#764ba2',
                    borderRadius: 8,
                    maxBarThickness: 42
                }]
            },
            options: {
                responsive: true, maintainAspectRatio: true,
                plugins: { legend: { display: false } },
                scales: {
                    y: { beginAtZero: true, ticks: { precision: 0 }, grid: { color: '#eef1f8' } },
                    x: { grid: { display: false } }
                }
            }
        });
    }
}

function _grafNotaVacia(panel, mensaje, canvas) {
    if (panel && !panel.querySelector('.empty-chart-note')) {
        const div = document.createElement('div');
        div.className = 'empty-chart-note';
        div.innerHTML = '<i class="fas fa-chart-line me-2"></i>' + (mensaje || 'Sin datos todavía');
        panel.appendChild(div);
    }
    if (canvas) canvas.style.display = 'none';
}

function _graflimpiaPanel(panel, canvas) {
    if (panel) {
        const nota = panel.querySelector('.empty-chart-note');
        if (nota) nota.remove();
    }
    canvas.style.display = '';
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