// ==================== JEFE ARCHIVO - VARIABLES GLOBALES ====================
let listaExpedientes = [];

// ==================== JEFE ARCHIVO - INICIALIZACIÓN ====================
document.addEventListener('DOMContentLoaded', function() {
    const sesion = verificarSesion();
    if (!sesion || sesion.rol !== 'JEFE_ARCHIVO') {
        if (sesion) redirigirPorRol(sesion.rol);
        return;
    }
    
    document.getElementById('userName').innerText = localStorage.getItem('username');
    cargarModoOscuro();
    cargarDashboardArchivo();
    cargarExpedientes();
    cargarPacientesArchivo();
    cargarAuditoria();
    
    // Eventos de filtros
    document.getElementById('buscarExpediente')?.addEventListener('input', cargarExpedientes);
    document.getElementById('filtroEstadoExpediente')?.addEventListener('change', cargarExpedientes);
    document.getElementById('buscarPacienteArchivo')?.addEventListener('input', cargarPacientesArchivo);
    document.getElementById('auditoriaFecha')?.addEventListener('change', cargarAuditoria);
    document.getElementById('auditoriaTipo')?.addEventListener('change', cargarAuditoria);
    
    // Tabs
    document.querySelectorAll('[data-tab]').forEach(btn => {
        btn.addEventListener('click', () => {
            const tab = btn.getAttribute('data-tab');
            document.querySelectorAll('.tab-content').forEach(c => c.style.display = 'none');
            document.getElementById(tab).style.display = 'block';
            document.querySelectorAll('.nav-link').forEach(n => n.classList.remove('active'));
            btn.classList.add('active');
            
            if (tab === 'dashboard') cargarDashboardArchivo();
            if (tab === 'expedientes') cargarExpedientes();
            if (tab === 'pacientes') cargarPacientesArchivo();
            if (tab === 'auditoria') cargarAuditoria();
        });
    });
});

// ==================== JEFE ARCHIVO - HELPERS ====================
function badgeArchivo(estado) {
    const estadoClass = estado === 'ACTIVO' ? 'archivo-activo' : 
                       estado === 'INACTIVO' ? 'archivo-inactivo' : 
                       estado === 'ARCHIVADO' ? 'archivo-archivado' : 'archivo-inactivo';
    return `<span class="archivo-badge ${estadoClass}">${estado}</span>`;
}

// ==================== JEFE ARCHIVO - DASHBOARD ====================
async function cargarDashboardArchivo() {
    setLoading(true);
    try {
        const res = await authFetch('/api/expedientes');
        if (res.ok) {
            listaExpedientes = await res.json();
            
            const total = listaExpedientes.length;
            const activos = listaExpedientes.filter(e => e.estado === 'ACTIVO').length;
            const archivados = listaExpedientes.filter(e => e.estado === 'ARCHIVADO').length;
            
            document.getElementById('totalExpedientes').innerText = total;
            document.getElementById('expedientesActivos').innerText = activos;
            document.getElementById('expedientesArchivados').innerText = archivados;
            
            // Conteo real de notas médicas registradas (desde la bitácora de auditoría)
            const resNotas = await authFetch('/api/auditoria?accion=NOTA_MEDICA').catch(() => null);
            if (resNotas && resNotas.ok) {
                const auditoria = await resNotas.json();
                document.getElementById('notasRegistradas').innerText = auditoria.total || 0;
            }
            
            // Últimos expedientes modificados (reales, por fecha de actualización)
            const ultimos = listaExpedientes.slice(0, 5);
            document.getElementById('tbodyUltimosExpedientes').innerHTML = ultimos.length
                ? ultimos.map(e => `
                    <tr>
                        <td>${e.pacienteNombre || 'N/A'}</td>
                        <td>${e.fechaActualizacion ? new Date(e.fechaActualizacion).toLocaleDateString('es-MX') : 'N/A'}</td>
                        <td>${badgeArchivo(e.estado)}</td>
                        <td>
                            <button class="btn btn-sm btn-primary" onclick="verExpedientePaciente(${e.pacienteId})">Ver</button>
                        </td>
                    </tr>
                `).join('')
                : '<tr><td colspan="4" class="text-center text-muted">No hay expedientes registrados</td></tr>';
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

// ==================== JEFE ARCHIVO - EXPEDIENTES ====================
async function cargarExpedientes() {
    setLoading(true);
    try {
        const res = await authFetch('/api/expedientes');
        if (res.ok) {
            listaExpedientes = await res.json();
            
            const termino = document.getElementById('buscarExpediente')?.value || '';
            const estado = document.getElementById('filtroEstadoExpediente')?.value;
            
            let filtered = listaExpedientes;
            if (termino.length > 1) {
                filtered = filtered.filter(e => (e.pacienteNombre || '').toLowerCase().includes(termino.toLowerCase()));
            }
            if (estado) {
                filtered = filtered.filter(e => e.estado === estado);
            }
            
            document.getElementById('tbodyExpedientes').innerHTML = filtered.length
                ? filtered.map(e => `
                    <tr>
                        <td>${e.pacienteId}</td>
                        <td>${e.pacienteNombre || 'N/A'}</td>
                        <td>${e.pacienteTelefono || 'N/A'}</td>
                        <td>${e.pacienteCorreo || 'N/A'}</td>
                        <td>${badgeArchivo(e.estado)}</td>
                        <td>${e.numeroNotas || 0}</td>
                        <td>${e.fechaActualizacion ? new Date(e.fechaActualizacion).toLocaleDateString('es-MX') : 'N/A'}</td>
                        <td>
                            <button class="btn btn-sm btn-primary" onclick="editarExpediente(${e.pacienteId}, '${(e.pacienteNombre || '').replace(/'/g, "\\'")}')">Gestionar</button>
                            <button class="btn btn-sm btn-info" onclick="verExpedientePaciente(${e.pacienteId})">Ver</button>
                        </td>
                    </tr>
                `).join('')
                : '<tr><td colspan="8" class="text-center text-muted">No hay expedientes que coincidan con el filtro</td></tr>';
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

function editarExpediente(pacienteId, nombre) {
    const expediente = listaExpedientes.find(e => e.pacienteId === pacienteId);
    
    document.getElementById('expedientePacienteId').value = pacienteId;
    document.getElementById('expedientePacienteNombre').innerText = nombre || `Paciente ${pacienteId}`;
    document.getElementById('expedienteEstado').value = expediente?.estado || 'ACTIVO';
    document.getElementById('expedienteNotas').value = expediente?.observaciones || '';
    new bootstrap.Modal(document.getElementById('expedienteModal')).show();
}

async function guardarExpediente() {
    const pacienteId = document.getElementById('expedientePacienteId').value;
    const estado = document.getElementById('expedienteEstado').value;
    const notas = document.getElementById('expedienteNotas').value;
    
    setLoading(true);
    try {
        const res = await authFetch('/api/expedientes', {
            method: 'POST',
            body: JSON.stringify({ pacienteId: Number(pacienteId), estado, observaciones: notas })
        });
        
        if (res.ok) {
            mostrarMensaje(`✅ Expediente del paciente ${pacienteId} actualizado a ${estado}`, 'success');
            bootstrap.Modal.getInstance(document.getElementById('expedienteModal')).hide();
            cargarExpedientes();
            cargarDashboardArchivo();
            cargarPacientesArchivo();
        } else {
            const error = await res.text();
            mostrarMensaje('❌ ' + error, 'danger');
        }
    } catch(e) {
        mostrarMensaje('Error de conexión', 'danger');
    }
    setLoading(false);
}

async function verExpedientePaciente(pacienteId) {
    setLoading(true);
    try {
        const res = await authFetch(`/api/expedientes/paciente/${pacienteId}`);
        if (!res.ok) {
            mostrarMensaje('No se pudo cargar el expediente', 'danger');
            setLoading(false);
            return;
        }
        
        const expediente = await res.json();
        
        // Notas clínicas del paciente (acceso de lectura de Archivo Clínico)
        let notasHtml = '';
        const resNotas = await authFetch(`/api/notas/doctor/paciente/${pacienteId}`).catch(() => null);
        if (resNotas && resNotas.ok) {
            const notas = await resNotas.json();
            notasHtml = notas.length
                ? notas.slice(0, 10).map(n => `
                    <li class="list-group-item">
                        <strong>${n.tipo}</strong> — ${new Date(n.fechaCreacion).toLocaleString('es-MX')}
                        <br><small>${n.diagnostico || n.medicamentos || n.recomendaciones || ''}</small>
                    </li>
                `).join('')
                : '<li class="list-group-item text-muted">Sin notas clínicas registradas</li>';
        }
        
        const modalHtml = `
            <div class="modal fade" id="verExpedienteModal" tabindex="-1">
                <div class="modal-dialog modal-lg">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title">Expediente: ${expediente.pacienteNombre || 'Paciente ' + pacienteId}</h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body">
                            <p><strong>Estado del expediente:</strong> ${badgeArchivo(expediente.estado)}</p>
                            <p><strong>Observaciones de archivo:</strong> ${expediente.observaciones || 'Ninguna'}</p>
                            <p><strong>Última actualización:</strong> ${expediente.fechaActualizacion ? new Date(expediente.fechaActualizacion).toLocaleString('es-MX') : 'N/A'}</p>
                            <h6 class="mt-3">Notas clínicas del paciente</h6>
                            <ul class="list-group">${notasHtml}</ul>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cerrar</button>
                        </div>
                    </div>
                </div>
            </div>
        `;
        document.getElementById('verExpedienteModal')?.remove();
        document.body.insertAdjacentHTML('beforeend', modalHtml);
        new bootstrap.Modal(document.getElementById('verExpedienteModal')).show();
    } catch(e) {
        mostrarMensaje('Error de conexión', 'danger');
    }
    setLoading(false);
}

// ==================== JEFE ARCHIVO - PACIENTES ====================
async function cargarPacientesArchivo() {
    setLoading(true);
    try {
        const res = await authFetch('/api/clientes');
        if (res.ok) {
            let pacientes = await res.json();
            const termino = document.getElementById('buscarPacienteArchivo')?.value || '';
            
            if (termino.length > 1) {
                pacientes = pacientes.filter(p => p.nombre.toLowerCase().includes(termino.toLowerCase()));
            }
            
            // Mapa de expedientes por pacienteId para estado real
            const expedientesPorPaciente = {};
            listaExpedientes.forEach(e => {
                expedientesPorPaciente[e.pacienteId] = e;
            });
            
            document.getElementById('tbodyPacientesArchivo').innerHTML = pacientes.map(p => {
                const expediente = expedientesPorPaciente[p.id];
                const estado = expediente?.estado || 'SIN_EXPEDIENTE';
                const notasCount = expediente?.numeroNotas || 0;
                return `
                    <tr>
                        <td>${p.id}</td>
                        <td>${p.nombre}</td>
                        <td>${p.telefono || 'N/A'}</td>
                        <td>${p.correo || 'N/A'}</td>
                        <td>${badgeArchivo(estado)}</td>
                        <td>${notasCount}</td>
                        <td>
                            <button class="btn btn-sm btn-primary" onclick="editarExpediente(${p.id}, '${(p.nombre || '').replace(/'/g, "\\'")}')">Gestionar</button>
                            <button class="btn btn-sm btn-info" onclick="verExpedientePaciente(${p.id})">Ver</button>
                        </td>
                    </tr>
                `;
            }).join('');
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

// ==================== JEFE ARCHIVO - AUDITORÍA ====================
async function cargarAuditoria() {
    setLoading(true);
    try {
        const fecha = document.getElementById('auditoriaFecha')?.value;
        const tipo = document.getElementById('auditoriaTipo')?.value;
        
        let url = '/api/auditoria';
        const params = [];
        if (fecha) params.push(`fecha=${fecha}`);
        if (tipo) params.push(`accion=${tipo}`);
        if (params.length) url += '?' + params.join('&');
        
        const res = await authFetch(url);
        if (res.ok) {
            const data = await res.json();
            const registros = data.registros || [];
            
            document.getElementById('tbodyAuditoria').innerHTML = registros.length
                ? registros.map(r => `
                    <tr>
                        <td>${new Date(r.fecha).toLocaleString('es-MX')}</td>
                        <td>${r.usuario}</td>
                        <td><span class="badge ${r.accion === 'CREACION' ? 'bg-success' : r.accion === 'MODIFICACION' ? 'bg-warning' : r.accion === 'CANCELACION' || r.accion === 'LOGIN_FALLIDO' ? 'bg-danger' : 'bg-info'}">${r.accion}</span></td>
                        <td>${r.detalle}</td>
                    </tr>
                `).join('')
                : '<tr><td colspan="4" class="text-center text-muted">Sin registros de auditoría para el filtro seleccionado</td></tr>';
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}
