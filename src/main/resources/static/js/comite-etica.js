// ==================== COMITÉ DE ÉTICA - VARIABLES GLOBALES ====================
let listaCasos = [];
let listaQuejas = [];
let casoActual = null;

// ==================== COMITÉ DE ÉTICA - INICIALIZACIÓN ====================
document.addEventListener('DOMContentLoaded', function() {
    const sesion = verificarSesion();
    if (!sesion || sesion.rol !== 'COMITE_ETICA') {
        if (sesion) redirigirPorRol(sesion.rol);
        return;
    }
    
    document.getElementById('userName').innerText = localStorage.getItem('username');
    cargarModoOscuro();
    cargarDashboardEtica();
    cargarCasos();
    cargarPacientesEtica();
    cargarQuejas();
    
    // Eventos de filtros
    document.getElementById('buscarCaso')?.addEventListener('input', cargarCasos);
    document.getElementById('filtroEstadoCaso')?.addEventListener('change', cargarCasos);
    document.getElementById('buscarPacienteEtica')?.addEventListener('input', cargarPacientesEtica);
    document.getElementById('buscarQueja')?.addEventListener('input', cargarQuejas);
    document.getElementById('filtroEstadoQueja')?.addEventListener('change', cargarQuejas);
    
    // Tabs
    document.querySelectorAll('[data-tab]').forEach(btn => {
        btn.addEventListener('click', () => {
            const tab = btn.getAttribute('data-tab');
            document.querySelectorAll('.tab-content').forEach(c => c.style.display = 'none');
            document.getElementById(tab).style.display = 'block';
            document.querySelectorAll('.nav-link').forEach(n => n.classList.remove('active'));
            btn.classList.add('active');
            
            if (tab === 'dashboard') cargarDashboardEtica();
            if (tab === 'casos') cargarCasos();
            if (tab === 'pacientes') cargarPacientesEtica();
            if (tab === 'quejas') cargarQuejas();
        });
    });
});

// ==================== COMITÉ DE ÉTICA - HELPERS ====================
function badgeEstadoCaso(estado) {
    const estadoClass = estado === 'PENDIENTE' ? 'badge-pendiente' : 
                       estado === 'APROBADO' ? 'badge-aprobado' : 
                       estado === 'RECHAZADO' ? 'badge-rechazado' : 'badge-en-revision';
    return `<span class="badge-estado-caso ${estadoClass}">${estado}</span>`;
}

function badgeEstadoQueja(estado) {
    return `<span class="badge ${estado === 'PENDIENTE' ? 'bg-warning' : estado === 'EN_PROCESO' ? 'bg-info' : 'bg-success'}">${estado}</span>`;
}

// ==================== COMITÉ DE ÉTICA - DASHBOARD ====================
async function cargarDashboardEtica() {
    setLoading(true);
    try {
        const res = await authFetch('/api/etica/casos');
        if (res.ok) {
            listaCasos = await res.json();
            
            const total = listaCasos.length;
            const pendientes = listaCasos.filter(c => c.estado === 'PENDIENTE').length;
            const aprobados = listaCasos.filter(c => c.estado === 'APROBADO').length;
            const rechazados = listaCasos.filter(c => c.estado === 'RECHAZADO').length;
            
            document.getElementById('totalCasos').innerText = total;
            document.getElementById('casosPendientes').innerText = pendientes;
            document.getElementById('casosAprobados').innerText = aprobados;
            document.getElementById('casosRechazados').innerText = rechazados;
            
            // Casos recientes (los más nuevos primero)
            const casosRecientes = listaCasos.slice(0, 5);
            
            document.getElementById('tbodyCasosRecientes').innerHTML = casosRecientes.length 
                ? casosRecientes.map(c => `
                    <tr>
                        <td>${c.id}</td>
                        <td>${c.pacienteNombre || 'N/A'}</td>
                        <td>${c.tipo}</td>
                        <td>${c.fecha || 'N/A'}</td>
                        <td>${badgeEstadoCaso(c.estado)}</td>
                        <td>
                            <button class="btn btn-sm btn-primary" onclick="abrirRevisionCaso(${c.id})">Revisar</button>
                        </td>
                    </tr>
                `).join('')
                : '<tr><td colspan="6" class="text-center text-muted">No hay casos registrados</td></tr>';
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

// ==================== COMITÉ DE ÉTICA - CASOS ====================
async function cargarCasos() {
    setLoading(true);
    try {
        const res = await authFetch('/api/etica/casos');
        if (res.ok) {
            listaCasos = await res.json();
            
            const termino = document.getElementById('buscarCaso')?.value || '';
            const estado = document.getElementById('filtroEstadoCaso')?.value;
            
            let filtered = listaCasos;
            if (termino.length > 1) {
                filtered = filtered.filter(c => (c.pacienteNombre || '').toLowerCase().includes(termino.toLowerCase()));
            }
            if (estado) {
                filtered = filtered.filter(c => c.estado === estado);
            }
            
            document.getElementById('tbodyCasos').innerHTML = filtered.length
                ? filtered.map(c => `
                    <tr>
                        <td>${c.id}</td>
                        <td>${c.pacienteNombre || 'N/A'}</td>
                        <td>${c.descripcion || ''}</td>
                        <td>${c.tipo}</td>
                        <td>${c.fecha || 'N/A'}</td>
                        <td>${badgeEstadoCaso(c.estado)}</td>
                        <td>
                            <button class="btn btn-sm btn-primary" onclick="abrirRevisionCaso(${c.id})">Revisar</button>
                        </td>
                    </tr>
                `).join('')
                : '<tr><td colspan="7" class="text-center text-muted">No hay casos que coincidan con el filtro</td></tr>';
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

function abrirRevisionCaso(id) {
    const caso = listaCasos.find(c => c.id === id);
    if (!caso) return;
    
    casoActual = caso;
    document.getElementById('casoId').value = id;
    document.getElementById('casoPaciente').innerText = caso.pacienteNombre || 'N/A';
    document.getElementById('casoFecha').innerText = caso.fecha || 'N/A';
    document.getElementById('casoTipo').innerText = caso.tipo;
    document.getElementById('casoDescripcion').innerText = caso.descripcion || '';
    document.getElementById('casoDictamen').value = caso.dictamen || '';
    document.getElementById('casoDecision').value = 'APROBADO';
    new bootstrap.Modal(document.getElementById('casoModal')).show();
}

async function guardarCaso() {
    if (!casoActual) return;
    
    const dictamen = document.getElementById('casoDictamen').value;
    const decision = document.getElementById('casoDecision').value;
    
    if (!dictamen) {
        mostrarMensaje('❌ Escribe un dictamen', 'danger');
        return;
    }
    
    setLoading(true);
    try {
        const res = await authFetch(`/api/etica/casos/${casoActual.id}/revisar`, {
            method: 'PUT',
            body: JSON.stringify({ dictamen, decision })
        });
        
        if (res.ok) {
            mostrarMensaje(`✅ Caso ${casoActual.id} ${decision === 'APROBADO' ? 'aprobado' : decision === 'RECHAZADO' ? 'rechazado' : 'enviado a revisión'}`, 'success');
            bootstrap.Modal.getInstance(document.getElementById('casoModal')).hide();
            casoActual = null;
            cargarCasos();
            cargarDashboardEtica();
        } else {
            const error = await res.text();
            mostrarMensaje('❌ ' + error, 'danger');
        }
    } catch(e) {
        mostrarMensaje('Error de conexión', 'danger');
    }
    setLoading(false);
}

// ==================== COMITÉ DE ÉTICA - PACIENTES ====================
async function cargarPacientesEtica() {
    setLoading(true);
    try {
        const res = await authFetch('/api/clientes');
        if (res.ok) {
            let pacientes = await res.json();
            const termino = document.getElementById('buscarPacienteEtica')?.value || '';
            
            if (termino.length > 1) {
                pacientes = pacientes.filter(p => p.nombre.toLowerCase().includes(termino.toLowerCase()));
            }
            
            // Conteo real de casos por paciente
            const casosPorPaciente = {};
            listaCasos.forEach(c => {
                if (c.pacienteId) casosPorPaciente[c.pacienteId] = (casosPorPaciente[c.pacienteId] || 0) + 1;
            });
            
            document.getElementById('tbodyPacientesEtica').innerHTML = pacientes.map(p => `
                <tr>
                    <td>${p.id}</td>
                    <td>${p.nombre}</td>
                    <td>${p.telefono || 'N/A'}</td>
                    <td>${p.correo || 'N/A'}</td>
                    <td>${casosPorPaciente[p.id] || 0}</td>
                    <td>
                        <button class="btn btn-sm btn-info" onclick="verCasosPaciente(${p.id})">Ver Casos</button>
                    </td>
                </tr>
            `).join('');
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

function verCasosPaciente(id) {
    const paciente = listaCasos.find(c => c.pacienteId === id)?.pacienteNombre || `paciente ${id}`;
    
    // Ir a la pestaña de casos y filtrar por el nombre del paciente
    document.querySelectorAll('.tab-content').forEach(c => c.style.display = 'none');
    document.getElementById('casos').style.display = 'block';
    document.querySelectorAll('.nav-link').forEach(n => n.classList.remove('active'));
    document.querySelector('[data-tab="casos"]')?.classList.add('active');
    document.getElementById('buscarCaso').value = paciente;
    cargarCasos();
}

// ==================== COMITÉ DE ÉTICA - QUEJAS ====================
async function cargarQuejas() {
    setLoading(true);
    try {
        const res = await authFetch('/api/etica/quejas');
        if (res.ok) {
            listaQuejas = await res.json();
            
            const termino = document.getElementById('buscarQueja')?.value || '';
            const estado = document.getElementById('filtroEstadoQueja')?.value;
            
            let filtered = listaQuejas;
            if (termino.length > 1) {
                filtered = filtered.filter(q => (q.pacienteNombre || '').toLowerCase().includes(termino.toLowerCase()));
            }
            if (estado) {
                filtered = filtered.filter(q => q.estado === estado);
            }
            
            document.getElementById('tbodyQuejas').innerHTML = filtered.length
                ? filtered.map(q => `
                    <tr>
                        <td>${q.id}</td>
                        <td>${q.pacienteNombre || 'N/A'}</td>
                        <td>${q.asunto}</td>
                        <td>${q.fecha || 'N/A'}</td>
                        <td>${badgeEstadoQueja(q.estado)}</td>
                        <td>
                            ${q.estado !== 'RESUELTO' 
                                ? `<button class="btn btn-sm btn-primary" onclick="atenderQueja(${q.id})">Atender</button>`
                                : `<span class="text-muted small" title="${q.resolucion || ''}">Resuelta</span>`}
                        </td>
                    </tr>
                `).join('')
                : '<tr><td colspan="6" class="text-center text-muted">No hay quejas registradas</td></tr>';
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

function atenderQueja(id) {
    const queja = listaQuejas.find(q => q.id === id);
    if (!queja) return;
    
    // Modal dinámico de atención de queja
    const modalHtml = `
        <div class="modal fade" id="quejaModal" tabindex="-1">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">Atender queja #${queja.id}</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <p><strong>Paciente:</strong> ${queja.pacienteNombre || 'N/A'}</p>
                        <p><strong>Asunto:</strong> ${queja.asunto}</p>
                        <p><strong>Descripción:</strong> ${queja.descripcion || 'Sin descripción'}</p>
                        <label class="form-label"><strong>Resolución</strong> (vacía = pasar a EN_PROCESO):</label>
                        <textarea class="form-control" id="quejaResolucion" rows="3" placeholder="Describe la acción tomada..."></textarea>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
                        <button type="button" class="btn btn-primary" onclick="confirmarResolverQueja(${queja.id})">Guardar</button>
                    </div>
                </div>
            </div>
        </div>
    `;
    document.getElementById('quejaModal')?.remove();
    document.body.insertAdjacentHTML('beforeend', modalHtml);
    new bootstrap.Modal(document.getElementById('quejaModal')).show();
}

async function confirmarResolverQueja(id) {
    const resolucion = document.getElementById('quejaResolucion').value;
    
    setLoading(true);
    try {
        const res = await authFetch(`/api/etica/quejas/${id}/resolver`, {
            method: 'PUT',
            body: JSON.stringify({ resolucion })
        });
        
        if (res.ok) {
            const queja = await res.json();
            mostrarMensaje(`✅ Queja #${id} actualizada a ${queja.estado}`, 'success');
            bootstrap.Modal.getInstance(document.getElementById('quejaModal')).hide();
            cargarQuejas();
        } else {
            const error = await res.text();
            mostrarMensaje('❌ ' + error, 'danger');
        }
    } catch(e) {
        mostrarMensaje('Error de conexión', 'danger');
    }
    setLoading(false);
}
