// ==================== ADMINISTRATIVO - VARIABLES GLOBALES ====================
let todasLasCitasAdmin = [];

// ==================== ADMINISTRATIVO - INICIALIZACIÓN ====================
document.addEventListener('DOMContentLoaded', function() {
    const sesion = verificarSesion();
    if (!sesion || sesion.rol !== 'ADMINISTRATIVO') {
        if (sesion) redirigirPorRol(sesion.rol);
        return;
    }
    
    document.getElementById('userName').innerText = localStorage.getItem('username');
    cargarModoOscuro();
    cargarDashboardAdmin();
    cargarCitasAdmin();
    cargarPacientesAdmin();
    cargarFacturas();
    
    // Eventos de filtros
    document.getElementById('filtroFechaAdmin')?.addEventListener('change', cargarCitasAdmin);
    document.getElementById('filtroEstadoAdmin')?.addEventListener('change', cargarCitasAdmin);
    document.getElementById('filtroDepartamentoAdmin')?.addEventListener('change', cargarCitasAdmin);
    document.getElementById('buscarPacienteAdmin')?.addEventListener('input', cargarPacientesAdmin);
    
    // Tabs
    document.querySelectorAll('[data-tab]').forEach(btn => {
        btn.addEventListener('click', () => {
            const tab = btn.getAttribute('data-tab');
            document.querySelectorAll('.tab-content').forEach(c => c.style.display = 'none');
            document.getElementById(tab).style.display = 'block';
            document.querySelectorAll('.nav-link').forEach(n => n.classList.remove('active'));
            btn.classList.add('active');
            
            if (tab === 'dashboard') cargarDashboardAdmin();
            if (tab === 'citas') cargarCitasAdmin();
            if (tab === 'facturacion') cargarFacturas();
            if (tab === 'pacientes') cargarPacientesAdmin();
        });
    });
});

// ==================== ADMINISTRATIVO - DASHBOARD ====================
async function cargarDashboardAdmin() {
    setLoading(true);
    try {
        const resCitas = await authFetch('/api/citas');
        if (resCitas.ok) {
            todasLasCitasAdmin = await resCitas.json();
            const hoy = new Date().toISOString().split('T')[0];
            const semanaAtras = new Date();
            semanaAtras.setDate(semanaAtras.getDate() - 7);
            const semanaStr = semanaAtras.toISOString().split('T')[0];
            
            const citasHoy = todasLasCitasAdmin.filter(c => c.fecha === hoy && c.estado !== 'CANCELADA');
            const ingresosHoy = citasHoy.filter(c => c.estado === 'COMPLETADA').reduce((sum, c) => sum + (c.precio || 0), 0);
            const citasSemana = todasLasCitasAdmin.filter(c => c.fecha >= semanaStr && c.fecha <= hoy && c.estado !== 'CANCELADA');
            
            document.getElementById('citasHoy').innerText = citasHoy.length;
            document.getElementById('ingresosHoy').innerHTML = '$' + ingresosHoy.toLocaleString('es-MX');
            document.getElementById('citasSemana').innerText = citasSemana.length;
            
            // Citas del día
            document.getElementById('tbodyCitasHoyAdmin').innerHTML = citasHoy.slice(0, 20).map(c => `
                <tr>
                    <td>${c.fecha || 'N/A'}</td>
                    <td>${c.hora || 'N/A'}</td>
                    <td>${c.cliente?.nombre || 'N/A'}</td>
                    <td>${c.usuario?.nombreCompleto || c.usuario?.username || 'N/A'}</td>
                    <td>${c.servicio || 'N/A'}</td>
                    <td class="precio-text">${formatearPrecio(c.precio)}</td>
                    <td>${obtenerBadgeEstado(c.estado)}</td>
                </tr>
            `).join('');
        }
        
        const resPacientes = await authFetch('/api/clientes');
        if (resPacientes.ok) {
            const pacientes = await resPacientes.json();
            document.getElementById('totalPacientes').innerText = pacientes.length;
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

// ==================== ADMINISTRATIVO - CITAS ====================
async function cargarCitasAdmin() {
    setLoading(true);
    try {
        const res = await authFetch('/api/citas');
        if (res.ok) {
            let citas = await res.json();
            
            const fechaFiltro = document.getElementById('filtroFechaAdmin')?.value;
            const estadoFiltro = document.getElementById('filtroEstadoAdmin')?.value;
            const deptoFiltro = document.getElementById('filtroDepartamentoAdmin')?.value;
            
            if (fechaFiltro) citas = citas.filter(c => c.fecha === fechaFiltro);
            if (estadoFiltro) citas = citas.filter(c => c.estado === estadoFiltro);
            if (deptoFiltro) citas = citas.filter(c => c.departamento === deptoFiltro);
            
            document.getElementById('tbodyCitasAdmin').innerHTML = citas.map(c => `
                <tr>
                    <td>${c.id}</td>
                    <td>${c.fecha || 'N/A'}</td>
                    <td>${c.hora || 'N/A'}</td>
                    <td>${c.cliente?.nombre || 'N/A'}</td>
                    <td>${c.usuario?.nombreCompleto || c.usuario?.username || 'N/A'}</td>
                    <td>${c.servicio || 'N/A'}</td>
                    <td class="precio-text">${formatearPrecio(c.precio)}</td>
                    <td><span class="badge-departamento ${c.departamento === 'PSICOLOGIA' ? 'badge-psicologia' : c.departamento === 'PSIQUIATRIA' ? 'badge-psiquiatria' : c.departamento === 'TRABAJO_SOCIAL' ? 'badge-trabajo-social' : c.departamento === 'ENFERMERIA' ? 'badge-enfermeria' : 'badge-administracion'}">${c.departamento || 'N/A'}</span></td>
                    <td>${obtenerBadgeEstado(c.estado)}</td>
                    <td>
                        ${c.estado === 'COMPLETADA' && c.precio > 0 ? 
                            `<button class="btn btn-sm btn-success" onclick="abrirModalPago(${c.id}, '${c.cliente?.nombre || ''}', '${c.servicio || ''}', ${c.precio || 0})">Cobrar</button>` : ''}
                    </td>
                </tr>
            `).join('');
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

// ==================== ADMINISTRATIVO - FACTURACIÓN ====================
async function cargarFacturas() {
    setLoading(true);
    try {
        const fechaInicio = document.getElementById('facturaFechaInicio')?.value || '2024-01-01';
        const fechaFin = document.getElementById('facturaFechaFin')?.value || new Date().toISOString().split('T')[0];
        
        const res = await authFetch('/api/citas');
        if (res.ok) {
            let citas = await res.json();
            citas = citas.filter(c => c.fecha >= fechaInicio && c.fecha <= fechaFin);
            
            const facturas = citas.filter(c => c.estado === 'COMPLETADA');
            const totalFacturado = facturas.reduce((sum, c) => sum + (c.precio || 0), 0);
            
            document.getElementById('facturaTotal').innerHTML = '$' + totalFacturado.toLocaleString('es-MX');
            document.getElementById('facturaCantidad').innerText = facturas.length;
            document.getElementById('facturaPagadas').innerText = facturas.length; // Simulado
            
            document.getElementById('tbodyFacturas').innerHTML = facturas.slice(0, 50).map((c, i) => `
                <tr>
                    <td>${i + 1}</td>
                    <td>${c.fecha || 'N/A'}</td>
                    <td>${c.cliente?.nombre || 'N/A'}</td>
                    <td>${c.servicio || 'N/A'}</td>
                    <td class="precio-text">${formatearPrecio(c.precio)}</td>
                    <td><span class="badge bg-success">Pagado</span></td>
                    <td>
                        <button class="btn btn-sm btn-outline-primary" onclick="exportarFactura(${c.id})"><i class="fas fa-print"></i></button>
                    </td>
                </tr>
            `).join('');
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

function exportarFactura(id) {
    mostrarMensaje(`📄 Factura ${id} exportada (simulación)`, 'info');
}

// ==================== ADMINISTRATIVO - PACIENTES ====================
async function cargarPacientesAdmin() {
    setLoading(true);
    try {
        const res = await authFetch('/api/clientes');
        if (res.ok) {
            let pacientes = await res.json();
            
            const termino = document.getElementById('buscarPacienteAdmin')?.value || '';
            if (termino.length > 1) {
                pacientes = pacientes.filter(p => p.nombre.toLowerCase().includes(termino.toLowerCase()));
            }
            
            const resCitas = await authFetch('/api/citas');
            const todasCitas = await resCitas.json();
            
            document.getElementById('tbodyPacientesAdmin').innerHTML = pacientes.map(p => {
                const citasPaciente = todasCitas.filter(c => c.cliente?.id === p.id);
                const totalGastado = citasPaciente.filter(c => c.estado === 'COMPLETADA').reduce((sum, c) => sum + (c.precio || 0), 0);
                return `
                    <tr>
                        <td>${p.id}</td>
                        <td>${p.nombre}</td>
                        <td>${p.telefono || 'N/A'}</td>
                        <td>${p.correo || 'N/A'}</td>
                        <td>${citasPaciente.length}</td>
                        <td class="precio-text">${formatearPrecio(totalGastado)}</td>
                        <td>
                            <button class="btn btn-sm btn-info" onclick="verHistorialPaciente(${p.id})">Historial</button>
                        </td>
                    </tr>
                `;
            }).join('');
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

function verHistorialPaciente(id) {
    mostrarMensaje(`📋 Historial del paciente ${id} (simulación)`, 'info');
}

// ==================== ADMINISTRATIVO - PAGOS ====================
function abrirModalPago(id, paciente, servicio, monto) {
    document.getElementById('pagoCitaId').value = id;
    document.getElementById('pagoPaciente').innerText = paciente;
    document.getElementById('pagoServicio').innerText = servicio;
    document.getElementById('pagoMonto').innerText = monto.toFixed(2);
    document.getElementById('pagoMetodo').value = 'Efectivo';
    document.getElementById('pagoReferencia').value = '';
    new bootstrap.Modal(document.getElementById('pagoModal')).show();
}

async function registrarPago() {
    const citaId = document.getElementById('pagoCitaId').value;
    const metodo = document.getElementById('pagoMetodo').value;
    const referencia = document.getElementById('pagoReferencia').value || 'Sin referencia';
    
    setLoading(true);
    try {
        // Simular registro de pago
        mostrarMensaje(`✅ Pago registrado para la cita ${citaId} - Método: ${metodo}`, 'success');
        bootstrap.Modal.getInstance(document.getElementById('pagoModal')).hide();
        cargarCitasAdmin();
        cargarDashboardAdmin();
        cargarFacturas();
    } catch(e) {
        mostrarMensaje('Error al registrar pago', 'danger');
    }
    setLoading(false);
}