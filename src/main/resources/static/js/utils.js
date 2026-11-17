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

// ==================== VISTAS CLÍNICAS COMPARTIDAS (SC-09) ====================
// Psicología y psiquiatría eran el mismo código con el nombre del rol cambiado.
// Todo lo de aquí abajo se arregla una vez y sirve a las dos vistas.

async function cargarCitasClinico(sufijo) {
    setLoading(true);
    try {
        const username = localStorage.getItem('username');
        const resUsuarios = await authFetch('/api/usuarios');
        if (resUsuarios.ok) {
            const usuarios = await resUsuarios.json();
            const yo = usuarios.find(u => u.username === username);

            const resCitas = await authFetch('/api/citas');
            if (resCitas.ok) {
                let citas = await resCitas.json();
                citas = citas.filter(c => c.usuario?.username === username || c.usuario?.id === yo?.id);

                const fechaFiltro = document.getElementById('filtroFecha' + sufijo)?.value;
                const estadoFiltro = document.getElementById('filtroEstado' + sufijo)?.value;

                if (fechaFiltro) citas = citas.filter(c => c.fecha === fechaFiltro);
                if (estadoFiltro) citas = citas.filter(c => c.estado === estadoFiltro);

                document.getElementById('tbodyCitas' + sufijo).innerHTML = citas.map(c => `
                    <tr>
                        <td>${c.id}</td>
                        <td>${c.fecha || 'N/A'}</td>
                        <td>${c.hora || 'N/A'}</td>
                        <td>${c.cliente?.nombre || 'N/A'}</td>
                        <td>${c.servicio || 'N/A'}</td>
                        <td>${obtenerBadgeEstado(c.estado)}</td>
                        <td>
                            ${c.estado === 'AGENDADA' ? `<button class="btn btn-sm btn-success" onclick="iniciarCita${sufijo}(${c.id})">Iniciar</button>` : ''}
                            ${c.estado === 'EN_PROGRESO' ? `<button class="btn btn-sm btn-warning" onclick="completarCita${sufijo}(${c.id})">Completar</button>` : ''}
                            ${c.estado !== 'CANCELADA' && c.estado !== 'COMPLETADA' ? `<button class="btn btn-sm btn-danger" onclick="cancelarCita${sufijo}(${c.id})">Cancelar</button>` : ''}
                        </td>
                    </tr>
                `).join('');
            }
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

async function cambiarEstadoCitaClinico(cfg) {
    if (confirm(cfg.confirmacion)) {
        setLoading(true);
        try {
            const res = await authFetch(cfg.url, { method: 'PUT' });
            if (res.ok) {
                window['cargarCitas' + cfg.sufijo]();
                window['cargarDashboard' + cfg.sufijo]();
                mostrarMensaje(cfg.mensaje);
            } else {
                mostrarMensaje('Error', 'danger');
            }
        } catch(e) {
            mostrarMensaje('Error de conexión', 'danger');
        }
        setLoading(false);
    }
}

// Los pacientes del clínico salen de sus ASIGNACIONES activas; si la clínica
// todavía los rastrea solo por citas, cae al criterio anterior.
async function obtenerPacientesClinico() {
    const username = localStorage.getItem('username');
    try {
        const res = await authFetch('/api/asignaciones/mias');
        if (res.ok) {
            const asigs = await res.json();
            if (Array.isArray(asigs)) {
                const clientes = asigs.map(a => a.cliente).filter(Boolean);
                const unicos = new Map();
                clientes.forEach(c => unicos.set(c.id, c));
                if (unicos.size > 0) return [...unicos.values()];
            }
        }
    } catch(e) { console.error(e); }

    const resUsuarios = await authFetch('/api/usuarios').catch(() => null);
    let yo = null;
    if (resUsuarios && resUsuarios.ok) {
        const usuarios = await resUsuarios.json();
        yo = usuarios.find(u => u.username === username);
    }
    const resCitas = await authFetch('/api/citas').catch(() => null);
    if (resCitas && resCitas.ok) {
        const todasCitas = await resCitas.json();
        const misCitas = todasCitas.filter(c => c.usuario?.username === username || c.usuario?.id === yo?.id);
        const pacientesIds = [...new Set(misCitas.map(c => c.cliente?.id))];
        const resPacientes = await authFetch('/api/clientes').catch(() => null);
        if (resPacientes && resPacientes.ok) {
            const pacientes = await resPacientes.json();
            return pacientes.filter(p => pacientesIds.includes(p.id));
        }
    }
    return [];
}

function seleccionarPacienteNotaClinico(cfg) {
    cfg.alSeleccionar(cfg.id);
    document.getElementById('notaPacienteId' + cfg.sufijo).value = cfg.id;
    document.getElementById('pacienteSeleccionadoNombre' + cfg.sufijo).innerHTML = cfg.nombre;
    document.getElementById('pacienteSeleccionadoInfo' + cfg.sufijo).style.display = 'block';
    document.getElementById('btnGuardarNota' + cfg.sufijo).disabled = false;

    document.getElementById('notaTipo' + cfg.sufijo).value = '';
    document.getElementById('notaContenido' + cfg.sufijo).value = '';

    cargarNotasPacienteClinico(cfg.sufijo, cfg.id);
}

async function cargarNotasPacienteClinico(sufijo, pacienteId) {
    setLoading(true);
    try {
        const res = await authFetch(`/api/notas/doctor/paciente/${pacienteId}`);
        if (res.ok) {
            const notas = await res.json();
            const container = document.getElementById('notasPacienteContainer' + sufijo);
            if (notas.length === 0) {
                container.innerHTML = '<div class="alert alert-info">No hay notas para este paciente.</div>';
            } else {
                container.innerHTML = notas.map(n => `
                    <div class="nota-card">
                        <div class="d-flex justify-content-between">
                            <strong class="text-primary">${n.tipo}:</strong>
                            <small class="text-muted">${new Date(n.fechaCreacion).toLocaleString()}</small>
                        </div>
                        <p class="mt-2 mb-0">${n.diagnostico || n.recomendaciones || n.medicamentos || ''}</p>
                        <hr class="my-2">
                        <small class="text-muted">Dr. ${n.doctor?.nombreCompleto || n.doctor?.username || 'Desconocido'}</small>
                    </div>
                `).join('');
            }
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

function verNotasPacienteClinico(cfg) {
    seleccionarPacienteNotaClinico(cfg);
    document.querySelector('[data-tab="notas"]').click();
}

async function guardarNotaClinico(cfg) {
    const pacienteId = document.getElementById('notaPacienteId' + cfg.sufijo).value;
    const tipo = document.getElementById('notaTipo' + cfg.sufijo).value;
    const contenido = document.getElementById('notaContenido' + cfg.sufijo).value;

    if (!pacienteId) {
        mostrarMensaje('❌ Selecciona un paciente', 'danger');
        return;
    }
    if (!tipo) {
        mostrarMensaje('❌ Selecciona un tipo de nota', 'danger');
        return;
    }
    if (!contenido) {
        mostrarMensaje('❌ Escribe el contenido', 'danger');
        return;
    }

    setLoading(true);
    try {
        let payload = { pacienteId: parseInt(pacienteId), tipo: tipo };
        const tiposRecomendacion = cfg.tiposRecomendacion || ['RECOMENDACION', 'CONSEJO', 'OBSERVACION'];
        if (tipo === 'DIAGNOSTICO') payload.diagnostico = contenido;
        else if (tiposRecomendacion.includes(tipo)) payload.recomendaciones = contenido;

        const res = await authFetch('/api/notas', { method: 'POST', body: JSON.stringify(payload) });
        if (res.ok) {
            document.getElementById('notaContenido' + cfg.sufijo).value = '';
            document.getElementById('notaTipo' + cfg.sufijo).value = '';
            await cargarNotasPacienteClinico(cfg.sufijo, parseInt(pacienteId));
            mostrarMensaje('✅ Nota guardada', 'success');
        } else {
            mostrarMensaje('Error al guardar', 'danger');
        }
    } catch(e) {
        mostrarMensaje('Error de conexión', 'danger');
    }
    setLoading(false);
}

async function cargarDashboardClinico(sufijo) {
    setLoading(true);
    try {
        const username = localStorage.getItem('username');
        const resUsuarios = await authFetch('/api/usuarios');
        if (resUsuarios.ok) {
            const usuarios = await resUsuarios.json();
            const yo = usuarios.find(u => u.username === username);

            const resCitas = await authFetch('/api/citas');
            if (resCitas.ok) {
                const todasCitas = await resCitas.json();
                const misCitas = todasCitas.filter(c => c.usuario?.username === username || c.usuario?.id === yo?.id);

                const hoy = new Date().toISOString().split('T')[0];
                const citasHoy = misCitas.filter(c => c.fecha === hoy && c.estado !== 'CANCELADA').length;
                const citasProgreso = misCitas.filter(c => c.estado === 'EN_PROGRESO').length;
                const citasCompletadas = misCitas.filter(c => c.estado === 'COMPLETADA').length;

                document.getElementById('citasHoy').innerText = citasHoy;
                document.getElementById('citasProgreso').innerText = citasProgreso;
                document.getElementById('citasCompletadas').innerText = citasCompletadas;

                const pacientesIds = new Set(misCitas.map(c => c.cliente?.id));
                document.getElementById('totalPacientes').innerText = pacientesIds.size;

                const proximas = misCitas.filter(c => c.fecha >= hoy && c.estado !== 'CANCELADA' && c.estado !== 'COMPLETADA').slice(0, 10);
                document.getElementById('tbodyProximasCitas' + sufijo).innerHTML = proximas.map(c => `
                    <tr>
                        <td>${c.fecha || 'N/A'}</td>
                        <td>${c.hora || 'N/A'}</td>
                        <td>${c.cliente?.nombre || 'N/A'}</td>
                        <td>${c.servicio || 'N/A'}</td>
                        <td>${obtenerBadgeEstado(c.estado)}</td>
                        <td>
                            <button class="btn btn-sm btn-success" onclick="iniciarCita${sufijo}(${c.id})">Iniciar</button>
                        </td>
                    </tr>
                `).join('');

                // Gráficas del panel una vez que ya tenemos misCitas
                await dibujarGraficasClinicas(misCitas);
            }
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}
