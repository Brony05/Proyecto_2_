// ==================== PSICÓLOGO - VARIABLES GLOBALES ====================
let pacienteSeleccionadoIdPsicologo = null;

// ==================== PSICÓLOGO - INICIALIZACIÓN ====================
document.addEventListener('DOMContentLoaded', function() {
    const sesion = verificarSesion();
    if (!sesion || sesion.rol !== 'PSICOLOGO') {
        if (sesion) redirigirPorRol(sesion.rol);
        return;
    }
    
    document.getElementById('userName').innerText = localStorage.getItem('username');
    document.getElementById('especialidadBadge').innerText = localStorage.getItem('especialidad') || 'Psicólogo General';
    cargarModoOscuro();
    cargarDashboardPsicologo();
    cargarCitasPsicologo();
    cargarPacientesPsicologo();
    cargarPacientesNotaPsicologo();
    
    // Eventos de filtros
    document.getElementById('filtroFechaPsicologo')?.addEventListener('change', cargarCitasPsicologo);
    document.getElementById('filtroEstadoPsicologo')?.addEventListener('change', cargarCitasPsicologo);
    document.getElementById('buscarPacientePsicologo')?.addEventListener('input', cargarPacientesPsicologo);
    
    // Tabs
    document.querySelectorAll('[data-tab]').forEach(btn => {
        btn.addEventListener('click', () => {
            const tab = btn.getAttribute('data-tab');
            document.querySelectorAll('.tab-content').forEach(c => c.style.display = 'none');
            document.getElementById(tab).style.display = 'block';
            document.querySelectorAll('.nav-link').forEach(n => n.classList.remove('active'));
            btn.classList.add('active');
            
            if (tab === 'dashboard') cargarDashboardPsicologo();
            if (tab === 'citas') cargarCitasPsicologo();
            if (tab === 'pacientes') cargarPacientesPsicologo();
            if (tab === 'notas') cargarPacientesNotaPsicologo();
        });
    });
});

// ==================== PSICÓLOGO - DASHBOARD ====================
async function cargarDashboardPsicologo() {
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
                
                // Pacientes únicos
                const pacientesIds = new Set(misCitas.map(c => c.cliente?.id));
                document.getElementById('totalPacientes').innerText = pacientesIds.size;
                
                // Próximas citas
                const proximas = misCitas.filter(c => c.fecha >= hoy && c.estado !== 'CANCELADA' && c.estado !== 'COMPLETADA').slice(0, 10);
                document.getElementById('tbodyProximasCitasPsicologo').innerHTML = proximas.map(c => `
                    <tr>
                        <td>${c.fecha || 'N/A'}</td>
                        <td>${c.hora || 'N/A'}</td>
                        <td>${c.cliente?.nombre || 'N/A'}</td>
                        <td>${c.servicio || 'N/A'}</td>
                        <td>${obtenerBadgeEstado(c.estado)}</td>
                        <td>
                            <button class="btn btn-sm btn-success" onclick="iniciarCitaPsicologo(${c.id})">Iniciar</button>
                        </td>
                    </tr>
                `).join('');
            }
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

// ==================== PSICÓLOGO - CITAS ====================
async function cargarCitasPsicologo() {
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
                
                const fechaFiltro = document.getElementById('filtroFechaPsicologo')?.value;
                const estadoFiltro = document.getElementById('filtroEstadoPsicologo')?.value;
                
                if (fechaFiltro) citas = citas.filter(c => c.fecha === fechaFiltro);
                if (estadoFiltro) citas = citas.filter(c => c.estado === estadoFiltro);
                
                document.getElementById('tbodyCitasPsicologo').innerHTML = citas.map(c => `
                    <tr>
                        <td>${c.id}</td>
                        <td>${c.fecha || 'N/A'}</td>
                        <td>${c.hora || 'N/A'}</td>
                        <td>${c.cliente?.nombre || 'N/A'}</td>
                        <td>${c.servicio || 'N/A'}</td>
                        <td>${obtenerBadgeEstado(c.estado)}</td>
                        <td>
                            ${c.estado === 'AGENDADA' ? `<button class="btn btn-sm btn-success" onclick="iniciarCitaPsicologo(${c.id})">Iniciar</button>` : ''}
                            ${c.estado === 'EN_PROGRESO' ? `<button class="btn btn-sm btn-warning" onclick="completarCitaPsicologo(${c.id})">Completar</button>` : ''}
                            ${c.estado !== 'CANCELADA' && c.estado !== 'COMPLETADA' ? `<button class="btn btn-sm btn-danger" onclick="cancelarCitaPsicologo(${c.id})">Cancelar</button>` : ''}
                        </td>
                    </tr>
                `).join('');
            }
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

async function iniciarCitaPsicologo(id) {
    if (confirm('¿Iniciar esta cita?')) {
        setLoading(true);
        try {
            const res = await authFetch(`/api/citas/${id}/estado?estado=EN_PROGRESO`, { method: 'PUT' });
            if (res.ok) {
                cargarCitasPsicologo();
                cargarDashboardPsicologo();
                mostrarMensaje('✅ Cita iniciada');
            } else {
                mostrarMensaje('Error', 'danger');
            }
        } catch(e) {
            mostrarMensaje('Error de conexión', 'danger');
        }
        setLoading(false);
    }
}

async function completarCitaPsicologo(id) {
    if (confirm('¿Marcar esta cita como completada?')) {
        setLoading(true);
        try {
            const res = await authFetch(`/api/citas/${id}/estado?estado=COMPLETADA`, { method: 'PUT' });
            if (res.ok) {
                cargarCitasPsicologo();
                cargarDashboardPsicologo();
                mostrarMensaje('✅ Cita completada');
            } else {
                mostrarMensaje('Error', 'danger');
            }
        } catch(e) {
            mostrarMensaje('Error de conexión', 'danger');
        }
        setLoading(false);
    }
}

async function cancelarCitaPsicologo(id) {
    if (confirm('¿Cancelar esta cita?')) {
        setLoading(true);
        try {
            const res = await authFetch(`/api/citas/${id}/cancelar`, { method: 'PUT' });
            if (res.ok) {
                cargarCitasPsicologo();
                cargarDashboardPsicologo();
                mostrarMensaje('✅ Cita cancelada');
            } else {
                mostrarMensaje('Error', 'danger');
            }
        } catch(e) {
            mostrarMensaje('Error de conexión', 'danger');
        }
        setLoading(false);
    }
}

// ==================== PSICÓLOGO - PACIENTES ====================
async function cargarPacientesPsicologo() {
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
                const pacientesIds = [...new Set(misCitas.map(c => c.cliente?.id))];
                
                const resPacientes = await authFetch('/api/clientes');
                if (resPacientes.ok) {
                    let pacientes = await resPacientes.json();
                    pacientes = pacientes.filter(p => pacientesIds.includes(p.id));
                    
                    const termino = document.getElementById('buscarPacientePsicologo')?.value || '';
                    if (termino.length > 1) {
                        pacientes = pacientes.filter(p => p.nombre.toLowerCase().includes(termino.toLowerCase()));
                    }
                    
                    document.getElementById('tbodyPacientesPsicologo').innerHTML = pacientes.map(p => {
                        const citasPaciente = misCitas.filter(c => c.cliente?.id === p.id);
                        const ultimaCita = citasPaciente.sort((a,b) => new Date(b.fecha) - new Date(a.fecha))[0];
                        return `
                            <tr>
                                <td>${p.id}</td>
                                <td>${p.nombre}</td>
                                <td>${p.telefono || 'N/A'}</td>
                                <td>${p.correo || 'N/A'}</td>
                                <td>${citasPaciente.length}</td>
                                <td>${ultimaCita ? ultimaCita.fecha + ' ' + ultimaCita.hora : '-'}</td>
                                <td>
                                    <button class="btn btn-sm btn-info" onclick="verNotasPacientePsicologo(${p.id}, '${p.nombre}')">Ver notas</button>
                                </td>
                            </tr>
                        `;
                    }).join('');
                }
            }
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

// ==================== PSICÓLOGO - NOTAS CLÍNICAS ====================
async function cargarPacientesNotaPsicologo() {
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
                const pacientesIds = [...new Set(misCitas.map(c => c.cliente?.id))];
                
                const resPacientes = await authFetch('/api/clientes');
                if (resPacientes.ok) {
                    let pacientes = await resPacientes.json();
                    pacientes = pacientes.filter(p => pacientesIds.includes(p.id));
                    
                    const container = document.getElementById('listaPacientesNotaPsicologo');
                    if (pacientes.length === 0) {
                        container.innerHTML = '<div class="alert alert-warning">No tienes pacientes asignados</div>';
                    } else {
                        container.innerHTML = pacientes.map(p => `
                            <div class="list-group-item list-group-item-action" 
                                 onclick="seleccionarPacienteNotaPsicologo(${p.id}, '${p.nombre}')" 
                                 style="cursor: pointer; border-left: 4px solid #2e7d32; margin-bottom: 5px;">
                                <strong>${p.nombre}</strong><br>
                                <small class="text-muted">${p.correo} | ${p.telefono || 'Sin teléfono'}</small>
                            </div>
                        `).join('');
                    }
                }
            }
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

function seleccionarPacienteNotaPsicologo(id, nombre) {
    pacienteSeleccionadoIdPsicologo = id;
    document.getElementById('notaPacienteIdPsicologo').value = id;
    document.getElementById('pacienteSeleccionadoNombrePsicologo').innerHTML = nombre;
    document.getElementById('pacienteSeleccionadoInfoPsicologo').style.display = 'block';
    document.getElementById('btnGuardarNotaPsicologo').disabled = false;
    
    document.getElementById('notaTipoPsicologo').value = '';
    document.getElementById('notaContenidoPsicologo').value = '';
    
    cargarNotasPacientePsicologo(id);
}

async function cargarNotasPacientePsicologo(pacienteId) {
    setLoading(true);
    try {
        const res = await authFetch(`/api/notas/doctor/paciente/${pacienteId}`);
        if (res.ok) {
            const notas = await res.json();
            const container = document.getElementById('notasPacienteContainerPsicologo');
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

async function guardarNotaPsicologo() {
    const pacienteId = document.getElementById('notaPacienteIdPsicologo').value;
    const tipo = document.getElementById('notaTipoPsicologo').value;
    const contenido = document.getElementById('notaContenidoPsicologo').value;
    
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
        if (tipo === 'DIAGNOSTICO') payload.diagnostico = contenido;
        else if (tipo === 'RECOMENDACION' || tipo === 'CONSEJO' || tipo === 'OBSERVACION') payload.recomendaciones = contenido;
        
        const res = await authFetch('/api/notas', { method: 'POST', body: JSON.stringify(payload) });
        if (res.ok) {
            document.getElementById('notaContenidoPsicologo').value = '';
            document.getElementById('notaTipoPsicologo').value = '';
            await cargarNotasPacientePsicologo(parseInt(pacienteId));
            mostrarMensaje('✅ Nota guardada', 'success');
        } else {
            mostrarMensaje('Error al guardar', 'danger');
        }
    } catch(e) {
        mostrarMensaje('Error de conexión', 'danger');
    }
    setLoading(false);
}

function verNotasPacientePsicologo(id, nombre) {
    seleccionarPacienteNotaPsicologo(id, nombre);
    document.querySelector('[data-tab="notas"]').click();
}