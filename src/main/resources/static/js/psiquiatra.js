// ==================== PSIQUIATRA - VARIABLES GLOBALES ====================
let pacienteSeleccionadoIdPsiquiatra = null;
let pacienteSeleccionadoIdMedicamento = null;
let listaPacientesPsiquiatra = [];

// ==================== PSIQUIATRA - INICIALIZACIÓN ====================
document.addEventListener('DOMContentLoaded', function() {
    const sesion = verificarSesion();
    if (!sesion || sesion.rol !== 'PSIQUIATRA') {
        if (sesion) redirigirPorRol(sesion.rol);
        return;
    }
    
    document.getElementById('userName').innerText = localStorage.getItem('username');
    document.getElementById('especialidadBadge').innerText = localStorage.getItem('especialidad') || 'Psiquiatra';
    cargarModoOscuro();
    cargarDashboardPsiquiatra();
    cargarCitasPsiquiatra();
    cargarPacientesPsiquiatra();
    cargarPacientesNotaPsiquiatra();
    cargarPacientesMedicamento();
    
    // Eventos de filtros
    document.getElementById('filtroFechaPsiquiatra')?.addEventListener('change', cargarCitasPsiquiatra);
    document.getElementById('filtroEstadoPsiquiatra')?.addEventListener('change', cargarCitasPsiquiatra);
    document.getElementById('buscarPacientePsiquiatra')?.addEventListener('input', cargarPacientesPsiquiatra);
    
    // Tabs
    document.querySelectorAll('[data-tab]').forEach(btn => {
        btn.addEventListener('click', () => {
            const tab = btn.getAttribute('data-tab');
            document.querySelectorAll('.tab-content').forEach(c => c.style.display = 'none');
            document.getElementById(tab).style.display = 'block';
            document.querySelectorAll('.nav-link').forEach(n => n.classList.remove('active'));
            btn.classList.add('active');
            
            if (tab === 'dashboard') cargarDashboardPsiquiatra();
            if (tab === 'citas') cargarCitasPsiquiatra();
            if (tab === 'pacientes') cargarPacientesPsiquiatra();
            if (tab === 'medicamentos') cargarPacientesMedicamento();
            if (tab === 'notas') cargarPacientesNotaPsiquiatra();
        });
    });
});

// ==================== PSIQUIATRA - DASHBOARD ====================
async function cargarDashboardPsiquiatra() {
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
                document.getElementById('tbodyProximasCitasPsiquiatra').innerHTML = proximas.map(c => `
                    <tr>
                        <td>${c.fecha || 'N/A'}</td>
                        <td>${c.hora || 'N/A'}</td>
                        <td>${c.cliente?.nombre || 'N/A'}</td>
                        <td>${c.servicio || 'N/A'}</td>
                        <td>${obtenerBadgeEstado(c.estado)}</td>
                        <td><button class="btn btn-sm btn-success" onclick="iniciarCitaPsiquiatra(${c.id})">Iniciar</button></td>
                    </tr>
                `).join('');
            }
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

// ==================== PSIQUIATRA - CITAS ====================
async function cargarCitasPsiquiatra() {
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
                
                const fechaFiltro = document.getElementById('filtroFechaPsiquiatra')?.value;
                const estadoFiltro = document.getElementById('filtroEstadoPsiquiatra')?.value;
                
                if (fechaFiltro) citas = citas.filter(c => c.fecha === fechaFiltro);
                if (estadoFiltro) citas = citas.filter(c => c.estado === estadoFiltro);
                
                document.getElementById('tbodyCitasPsiquiatra').innerHTML = citas.map(c => `
                    <tr>
                        <td>${c.id}</td>
                        <td>${c.fecha || 'N/A'}</td>
                        <td>${c.hora || 'N/A'}</td>
                        <td>${c.cliente?.nombre || 'N/A'}</td>
                        <td>${c.servicio || 'N/A'}</td>
                        <td>${obtenerBadgeEstado(c.estado)}</td>
                        <td>
                            ${c.estado === 'AGENDADA' ? `<button class="btn btn-sm btn-success" onclick="iniciarCitaPsiquiatra(${c.id})">Iniciar</button>` : ''}
                            ${c.estado === 'EN_PROGRESO' ? `<button class="btn btn-sm btn-warning" onclick="completarCitaPsiquiatra(${c.id})">Completar</button>` : ''}
                            ${c.estado !== 'CANCELADA' && c.estado !== 'COMPLETADA' ? `<button class="btn btn-sm btn-danger" onclick="cancelarCitaPsiquiatra(${c.id})">Cancelar</button>` : ''}
                        </td>
                    </tr>
                `).join('');
            }
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

async function iniciarCitaPsiquiatra(id) {
    if (confirm('¿Iniciar esta cita?')) {
        setLoading(true);
        try {
            const res = await authFetch(`/api/citas/${id}/estado?estado=EN_PROGRESO`, { method: 'PUT' });
            if (res.ok) {
                cargarCitasPsiquiatra();
                cargarDashboardPsiquiatra();
                mostrarMensaje('✅ Cita iniciada');
            }
        } catch(e) { mostrarMensaje('Error', 'danger'); }
        setLoading(false);
    }
}

async function completarCitaPsiquiatra(id) {
    if (confirm('¿Marcar como completada?')) {
        setLoading(true);
        try {
            const res = await authFetch(`/api/citas/${id}/estado?estado=COMPLETADA`, { method: 'PUT' });
            if (res.ok) {
                cargarCitasPsiquiatra();
                cargarDashboardPsiquiatra();
                mostrarMensaje('✅ Cita completada');
            }
        } catch(e) { mostrarMensaje('Error', 'danger'); }
        setLoading(false);
    }
}

async function cancelarCitaPsiquiatra(id) {
    if (confirm('¿Cancelar esta cita?')) {
        setLoading(true);
        try {
            const res = await authFetch(`/api/citas/${id}/cancelar`, { method: 'PUT' });
            if (res.ok) {
                cargarCitasPsiquiatra();
                cargarDashboardPsiquiatra();
                mostrarMensaje('✅ Cita cancelada');
            }
        } catch(e) { mostrarMensaje('Error', 'danger'); }
        setLoading(false);
    }
}

// ==================== PSIQUIATRA - PACIENTES ====================
async function cargarPacientesPsiquiatra() {
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
                    
                    const termino = document.getElementById('buscarPacientePsiquiatra')?.value || '';
                    if (termino.length > 1) {
                        pacientes = pacientes.filter(p => p.nombre.toLowerCase().includes(termino.toLowerCase()));
                    }
                    
                    // Obtener medicamentos para cada paciente
                    const medicamentosPorPaciente = {};
                    for (const p of pacientes) {
                        const resMed = await authFetch(`/api/medicamentos/paciente/${p.id}`).catch(() => null);
                        if (resMed && resMed.ok) {
                            const meds = await resMed.json();
                            medicamentosPorPaciente[p.id] = meds.length;
                        }
                    }
                    
                    document.getElementById('tbodyPacientesPsiquiatra').innerHTML = pacientes.map(p => {
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
                                <td><span class="badge bg-info">${medicamentosPorPaciente[p.id] || 0}</span></td>
                                <td>
                                    <button class="btn btn-sm btn-primary" onclick="verMedicamentosPaciente(${p.id}, '${p.nombre}')">Medicamentos</button>
                                    <button class="btn btn-sm btn-info" onclick="verNotasPacientePsiquiatra(${p.id}, '${p.nombre}')">Notas</button>
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

// ==================== PSIQUIATRA - MEDICAMENTOS ====================
async function cargarPacientesMedicamento() {
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
                    listaPacientesPsiquiatra = pacientes;
                    
                    const container = document.getElementById('listaPacientesMedicamento');
                    if (pacientes.length === 0) {
                        container.innerHTML = '<div class="alert alert-warning">No tienes pacientes asignados</div>';
                    } else {
                        container.innerHTML = pacientes.map(p => `
                            <div class="list-group-item list-group-item-action" 
                                 onclick="seleccionarPacienteMedicamento(${p.id}, '${p.nombre}')" 
                                 style="cursor: pointer; border-left: 4px solid #0d47a1; margin-bottom: 5px;">
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

function seleccionarPacienteMedicamento(id, nombre) {
    pacienteSeleccionadoIdMedicamento = id;
    document.getElementById('medicamentoPacienteId').value = id;
    document.getElementById('pacienteSeleccionadoMedicamentoNombre').innerHTML = nombre;
    document.getElementById('pacienteSeleccionadoMedicamento').style.display = 'block';
    document.getElementById('btnGuardarMedicamento').disabled = false;
    
    document.getElementById('medicamentoNombre').value = '';
    document.getElementById('medicamentoDosis').value = '';
    document.getElementById('medicamentoFrecuencia').value = 'Diario';
    document.getElementById('medicamentoNotas').value = '';
    
    cargarMedicamentosPaciente(id);
}

async function cargarMedicamentosPaciente(pacienteId) {
    setLoading(true);
    try {
        // Crear endpoint para medicamentos (simulado por ahora)
        // Por ahora usamos notas de tipo MEDICAMENTO
        const res = await authFetch(`/api/notas/doctor/paciente/${pacienteId}`);
        if (res.ok) {
            const notas = await res.json();
            const medicamentos = notas.filter(n => n.tipo === 'MEDICAMENTO');
            const container = document.getElementById('medicamentosContainer');
            if (medicamentos.length === 0) {
                container.innerHTML = '<div class="alert alert-info">No hay medicamentos recetados para este paciente.</div>';
            } else {
                container.innerHTML = medicamentos.map(m => `
                    <div class="medicamento-card">
                        <div class="d-flex justify-content-between">
                            <strong>${m.medicamentos || 'Medicamento'}</strong>
                            <small class="text-muted">${new Date(m.fechaCreacion).toLocaleDateString()}</small>
                        </div>
                        <p class="mt-1 mb-0">${m.recomendaciones || ''}</p>
                        <small class="text-muted">Dr. ${m.doctor?.nombreCompleto || m.doctor?.username || 'Desconocido'}</small>
                    </div>
                `).join('');
            }
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

async function guardarMedicamento() {
    const pacienteId = document.getElementById('medicamentoPacienteId').value;
    const nombre = document.getElementById('medicamentoNombre').value;
    const dosis = document.getElementById('medicamentoDosis').value;
    const frecuencia = document.getElementById('medicamentoFrecuencia').value;
    const notas = document.getElementById('medicamentoNotas').value;
    
    if (!pacienteId) {
        mostrarMensaje('❌ Selecciona un paciente', 'danger');
        return;
    }
    if (!nombre || !dosis) {
        mostrarMensaje('❌ Completa nombre y dosis del medicamento', 'danger');
        return;
    }
    
    setLoading(true);
    try {
        const contenido = `💊 ${nombre}\n📊 Dosis: ${dosis}\n📅 Frecuencia: ${frecuencia}\n📝 Notas: ${notas || 'Sin indicaciones adicionales'}`;
        
        const res = await authFetch('/api/notas', {
            method: 'POST',
            body: JSON.stringify({
                pacienteId: parseInt(pacienteId),
                tipo: 'MEDICAMENTO',
                medicamentos: contenido
            })
        });
        
        if (res.ok) {
            document.getElementById('medicamentoNombre').value = '';
            document.getElementById('medicamentoDosis').value = '';
            document.getElementById('medicamentoNotas').value = '';
            await cargarMedicamentosPaciente(parseInt(pacienteId));
            mostrarMensaje('✅ Medicamento recetado correctamente', 'success');
        } else {
            mostrarMensaje('Error al guardar', 'danger');
        }
    } catch(e) {
        mostrarMensaje('Error de conexión', 'danger');
    }
    setLoading(false);
}

function verMedicamentosPaciente(id, nombre) {
    seleccionarPacienteMedicamento(id, nombre);
    document.querySelector('[data-tab="medicamentos"]').click();
}

// ==================== PSIQUIATRA - NOTAS CLÍNICAS ====================
async function cargarPacientesNotaPsiquiatra() {
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
                    
                    const container = document.getElementById('listaPacientesNotaPsiquiatra');
                    if (pacientes.length === 0) {
                        container.innerHTML = '<div class="alert alert-warning">No tienes pacientes asignados</div>';
                    } else {
                        container.innerHTML = pacientes.map(p => `
                            <div class="list-group-item list-group-item-action" 
                                 onclick="seleccionarPacienteNotaPsiquiatra(${p.id}, '${p.nombre}')" 
                                 style="cursor: pointer; border-left: 4px solid #0d47a1; margin-bottom: 5px;">
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

function seleccionarPacienteNotaPsiquiatra(id, nombre) {
    pacienteSeleccionadoIdPsiquiatra = id;
    document.getElementById('notaPacienteIdPsiquiatra').value = id;
    document.getElementById('pacienteSeleccionadoNombrePsiquiatra').innerHTML = nombre;
    document.getElementById('pacienteSeleccionadoInfoPsiquiatra').style.display = 'block';
    document.getElementById('btnGuardarNotaPsiquiatra').disabled = false;
    
    document.getElementById('notaTipoPsiquiatra').value = '';
    document.getElementById('notaContenidoPsiquiatra').value = '';
    
    cargarNotasPacientePsiquiatra(id);
}

async function cargarNotasPacientePsiquiatra(pacienteId) {
    setLoading(true);
    try {
        const res = await authFetch(`/api/notas/doctor/paciente/${pacienteId}`);
        if (res.ok) {
            const notas = await res.json();
            const container = document.getElementById('notasPacienteContainerPsiquiatra');
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

async function guardarNotaPsiquiatra() {
    const pacienteId = document.getElementById('notaPacienteIdPsiquiatra').value;
    const tipo = document.getElementById('notaTipoPsiquiatra').value;
    const contenido = document.getElementById('notaContenidoPsiquiatra').value;
    
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
        else if (tipo === 'RECOMENDACION' || tipo === 'CONSEJO' || tipo === 'OBSERVACION' || tipo === 'EVOLUCION') {
            payload.recomendaciones = contenido;
        }
        
        const res = await authFetch('/api/notas', { method: 'POST', body: JSON.stringify(payload) });
        if (res.ok) {
            document.getElementById('notaContenidoPsiquiatra').value = '';
            document.getElementById('notaTipoPsiquiatra').value = '';
            await cargarNotasPacientePsiquiatra(parseInt(pacienteId));
            mostrarMensaje('✅ Nota guardada', 'success');
        } else {
            mostrarMensaje('Error al guardar', 'danger');
        }
    } catch(e) {
        mostrarMensaje('Error de conexión', 'danger');
    }
    setLoading(false);
}

function verNotasPacientePsiquiatra(id, nombre) {
    seleccionarPacienteNotaPsiquiatra(id, nombre);
    document.querySelector('[data-tab="notas"]').click();
}