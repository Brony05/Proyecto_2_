// ==================== ADMINISTRATIVO - VARIABLES GLOBALES ====================
let todasLasCitasAdmin = [];
let todosLosPagos = [];

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
    document.getElementById('facturaFechaInicio')?.addEventListener('change', cargarFacturas);
    document.getElementById('facturaFechaFin')?.addEventListener('change', cargarFacturas);
    
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

// ==================== ADMINISTRATIVO - HELPERS ====================
async function cargarPagos() {
    try {
        const res = await authFetch('/api/pagos');
        if (res.ok) {
            todosLosPagos = await res.json();
        }
        return todosLosPagos;
    } catch(e) {
        return [];
    }
}

function citasPagadas() {
    return new Set(todosLosPagos.map(p => p.citaId));
}

// ==================== ADMINISTRATIVO - DASHBOARD ====================
async function cargarDashboardAdmin() {
    setLoading(true);
    try {
        await cargarPagos();
        
        const resCitas = await authFetch('/api/citas');
        if (resCitas.ok) {
            todasLasCitasAdmin = await resCitas.json();
            const hoy = new Date().toISOString().split('T')[0];
            const semanaAtras = new Date();
            semanaAtras.setDate(semanaAtras.getDate() - 7);
            const semanaStr = semanaAtras.toISOString().split('T')[0];
            
            const citasHoy = todasLasCitasAdmin.filter(c => c.fecha === hoy && c.estado !== 'CANCELADA');
            
            // Ingresos reales: pagos cobrados hoy (por fecha de la cita)
            const pagadasHoy = todosLosPagos.filter(p => p.citaFecha === hoy);
            const ingresosHoy = pagadasHoy.reduce((sum, p) => sum + (p.monto || 0), 0);
            const citasSemana = todasLasCitasAdmin.filter(c => c.fecha >= semanaStr && c.fecha <= hoy && c.estado !== 'CANCELADA');
            
            document.getElementById('citasHoy').innerText = citasHoy.length;
            document.getElementById('ingresosHoy').innerHTML = '$' + ingresosHoy.toLocaleString('es-MX');
            document.getElementById('citasSemana').innerText = citasSemana.length;
            
            // Citas del día
            document.getElementById('tbodyCitasHoyAdmin').innerHTML = citasHoy.length
                ? citasHoy.slice(0, 20).map(c => `
                    <tr>
                        <td>${c.fecha || 'N/A'}</td>
                        <td>${c.hora || 'N/A'}</td>
                        <td>${c.cliente?.nombre || 'N/A'}</td>
                        <td>${c.usuario?.nombreCompleto || c.usuario?.username || 'N/A'}</td>
                        <td>${c.servicio || 'N/A'}</td>
                        <td class="precio-text">${formatearPrecio(c.precio)}</td>
                        <td>${obtenerBadgeEstado(c.estado)}</td>
                    </tr>
                `).join('')
                : '<tr><td colspan="7" class="text-center text-muted">No hay citas para hoy</td></tr>';
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
        await cargarPagos();
        
        const res = await authFetch('/api/citas');
        if (res.ok) {
            let citas = await res.json();
            
            const fechaFiltro = document.getElementById('filtroFechaAdmin')?.value;
            const estadoFiltro = document.getElementById('filtroEstadoAdmin')?.value;
            const deptoFiltro = document.getElementById('filtroDepartamentoAdmin')?.value;
            const pagadas = citasPagadas();
            
            if (fechaFiltro) citas = citas.filter(c => c.fecha === fechaFiltro);
            if (estadoFiltro) citas = citas.filter(c => c.estado === estadoFiltro);
            if (deptoFiltro) citas = citas.filter(c => c.departamento === deptoFiltro);
            
            document.getElementById('tbodyCitasAdmin').innerHTML = citas.length
                ? citas.map(c => {
                    const yaPagada = pagadas.has(c.id);
                    return `
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
                                ${c.estado === 'COMPLETADA' && c.precio > 0 && !yaPagada ? 
                                    `<button class="btn btn-sm btn-success" onclick="abrirModalPago(${c.id}, '${(c.cliente?.nombre || '').replace(/'/g, "\\'")}', '${(c.servicio || '').replace(/'/g, "\\'")}', ${c.precio || 0})">Cobrar</button>` : 
                                    (yaPagada ? '<span class="badge bg-success">Pagado</span>' : '')}
                            </td>
                        </tr>
                    `;
                }).join('')
                : '<tr><td colspan="10" class="text-center text-muted">No hay citas para el filtro seleccionado</td></tr>';
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
        
        // Pagos reales dentro del rango de fechas de atención
        const res = await authFetch(`/api/pagos/rango?inicio=${fechaInicio}&fin=${fechaFin}`);
        if (res.ok) {
            const pagos = await res.json();
            todosLosPagos = pagos.length ? pagos : todosLosPagos;
            
            const totalFacturado = pagos.reduce((sum, p) => sum + (p.monto || 0), 0);
            
            document.getElementById('facturaTotal').innerHTML = '$' + totalFacturado.toLocaleString('es-MX');
            document.getElementById('facturaCantidad').innerText = pagos.length;
            document.getElementById('facturaPagadas').innerText = pagos.length;
            
            document.getElementById('tbodyFacturas').innerHTML = pagos.length
                ? pagos.slice(0, 50).map(p => `
                    <tr>
                        <td>${p.id}</td>
                        <td>${p.citaFecha || 'N/A'}</td>
                        <td>${p.pacienteNombre || 'N/A'}</td>
                        <td>${p.servicio || 'N/A'}</td>
                        <td class="precio-text">${formatearPrecio(p.monto)}</td>
                        <td><span class="badge bg-info">${p.metodo || 'N/A'}</span></td>
                        <td><span class="badge bg-success">Pagado</span></td>
                        <td>
                            <button class="btn btn-sm btn-outline-primary" onclick="exportarFactura(${p.citaId})"><i class="fas fa-print"></i></button>
                        </td>
                    </tr>
                `).join('')
                : '<tr><td colspan="8" class="text-center text-muted">Sin pagos en el rango seleccionado</td></tr>';
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

function exportarFactura(citaId) {
    const pago = todosLosPagos.find(p => p.citaId === citaId);
    if (!pago) {
        mostrarMensaje('No hay pago registrado para esta cita', 'warning');
        return;
    }
    
    // Comprobante imprimible real
    const ventana = window.open('', '_blank', 'width=600,height=700');
    ventana.document.write(`
        <html>
        <head><title>Comprobante de pago - Cita #${citaId}</title>
        <style>
            body { font-family: Arial, sans-serif; padding: 24px; }
            h1 { font-size: 20px; border-bottom: 2px solid #1a237e; padding-bottom: 8px; }
            table { width: 100%; border-collapse: collapse; margin-top: 16px; }
            td { padding: 6px 4px; }
            td:first-child { font-weight: bold; width: 40%; }
            .total { font-size: 18px; font-weight: bold; border-top: 1px solid #333; padding-top: 8px; }
        </style></head>
        <body>
            <h1>Consultorio Psiquiátrico - Comprobante de pago</h1>
            <table>
                <tr><td>Folio:</td><td>PAGO-${String(pago.id).padStart(6, '0')}</td></tr>
                <tr><td>Paciente:</td><td>${pago.pacienteNombre || 'N/A'}</td></tr>
                <tr><td>Servicio:</td><td>${pago.servicio || 'N/A'}</td></tr>
                <tr><td>Fecha de atención:</td><td>${pago.citaFecha || 'N/A'}</td></tr>
                <tr><td>Método de pago:</td><td>${pago.metodo || 'N/A'}</td></tr>
                <tr><td>Referencia:</td><td>${pago.referencia || 'N/A'}</td></tr>
                <tr><td>Registrado por:</td><td>${pago.registradoPor || 'N/A'}</td></tr>
                <tr><td>Fecha de cobro:</td><td>${pago.fecha ? new Date(pago.fecha).toLocaleString('es-MX') : 'N/A'}</td></tr>
                <tr><td class="total">Total pagado:</td><td class="total">$${(pago.monto || 0).toFixed(2)} MXN</td></tr>
            </table>
            <p style="margin-top:24px; font-size:12px; color:#666;">Este documento es el comprobante oficial de su pago. Conserve para cualquier aclaración.</p>
        </body></html>
    `);
    ventana.document.close();
    ventana.focus();
    ventana.print();
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
            const todasCitas = await resCitas.ok ? await resCitas.json() : [];
            
            // Cobros reales por paciente (por nombre del pago)
            const cobradoPorPaciente = {};
            todosLosPagos.forEach(p => {
                if (p.pacienteNombre) {
                    cobradoPorPaciente[p.pacienteNombre] = (cobradoPorPaciente[p.pacienteNombre] || 0) + (p.monto || 0);
                }
            });
            
            document.getElementById('tbodyPacientesAdmin').innerHTML = pacientes.map(p => {
                const citasPaciente = todasCitas.filter(c => c.cliente?.id === p.id);
                const totalCobrado = cobradoPorPaciente[p.nombre] || 0;
                return `
                    <tr>
                        <td>${p.id}</td>
                        <td>${p.nombre}</td>
                        <td>${p.telefono || 'N/A'}</td>
                        <td>${p.correo || 'N/A'}</td>
                        <td>${citasPaciente.length}</td>
                        <td class="precio-text">${formatearPrecio(totalCobrado)}</td>
                        <td>
                            <button class="btn btn-sm btn-info" onclick="verHistorialPaciente(${p.id}, '${(p.nombre || '').replace(/'/g, "\\'")}')">Historial</button>
                        </td>
                    </tr>
                `;
            }).join('');
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

function verHistorialPaciente(id, nombre) {
    const citasPaciente = todasLasCitasAdmin.filter(c => c.cliente?.id === id);
    const pagosPaciente = todosLosPagos.filter(p => p.pacienteNombre === nombre);
    
    const modalHtml = `
        <div class="modal fade" id="historialModal" tabindex="-1">
            <div class="modal-dialog modal-lg">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">Historial: ${nombre || 'Paciente ' + id}</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <h6>Citas (${citasPaciente.length})</h6>
                        <table class="table table-sm">
                            <thead><tr><th>Fecha</th><th>Hora</th><th>Servicio</th><th>Estado</th></tr></thead>
                            <tbody>
                                ${citasPaciente.length ? citasPaciente.map(c => `
                                    <tr>
                                        <td>${c.fecha || ''}</td>
                                        <td>${c.hora || ''}</td>
                                        <td>${c.servicio || ''}</td>
                                        <td>${obtenerBadgeEstado(c.estado)}</td>
                                    </tr>
                                `).join('') : '<tr><td colspan="4" class="text-muted text-center">Sin citas</td></tr>'}
                            </tbody>
                        </table>
                        <h6 class="mt-3">Cobros (${pagosPaciente.length})</h6>
                        <table class="table table-sm">
                            <thead><tr><th>Folio</th><th>Fecha cita</th><th>Monto</th><th>Método</th></tr></thead>
                            <tbody>
                                ${pagosPaciente.length ? pagosPaciente.map(p => `
                                    <tr>
                                        <td>PAGO-${String(p.id).padStart(6, '0')}</td>
                                        <td>${p.citaFecha || ''}</td>
                                        <td>${formatearPrecio(p.monto)}</td>
                                        <td>${p.metodo || ''}</td>
                                    </tr>
                                `).join('') : '<tr><td colspan="4" class="text-muted text-center">Sin cobros</td></tr>'}
                            </tbody>
                        </table>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cerrar</button>
                    </div>
                </div>
            </div>
        </div>
    `;
    document.getElementById('historialModal')?.remove();
    document.body.insertAdjacentHTML('beforeend', modalHtml);
    new bootstrap.Modal(document.getElementById('historialModal')).show();
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
    const monto = parseFloat(document.getElementById('pagoMonto').innerText);
    const metodo = document.getElementById('pagoMetodo').value;
    const referencia = document.getElementById('pagoReferencia').value || 'Sin referencia';
    
    setLoading(true);
    try {
        const res = await authFetch('/api/pagos', {
            method: 'POST',
            body: JSON.stringify({ citaId: Number(citaId), monto, metodo, referencia })
        });
        
        if (res.ok) {
            const pago = await res.json();
            mostrarMensaje(`✅ Pago registrado para la cita ${citaId} - Método: ${metodo} (Folio PAGO-${String(pago.id).padStart(6, '0')})`, 'success');
            bootstrap.Modal.getInstance(document.getElementById('pagoModal')).hide();
            cargarCitasAdmin();
            cargarDashboardAdmin();
            cargarFacturas();
        } else {
            const error = await res.text();
            mostrarMensaje('❌ ' + error, 'danger');
        }
    } catch(e) {
        mostrarMensaje('Error al registrar pago', 'danger');
    }
    setLoading(false);
}
