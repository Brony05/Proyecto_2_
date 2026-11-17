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
    const avatarClin = document.getElementById('clinAvatar');
    if (avatarClin) avatarClin.innerText = (localStorage.getItem('username') || 'P').charAt(0).toUpperCase();
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
    return cargarDashboardClinico('Psiquiatra');
}

// ==================== PSIQUIATRA - CITAS ====================
async function cargarCitasPsiquiatra() {
    return cargarCitasClinico('Psiquiatra');
}

async function iniciarCitaPsiquiatra(id) {
    return cambiarEstadoCitaClinico({
        id, sufijo: 'Psiquiatra', url: `/api/citas/${id}/estado?estado=EN_PROGRESO`,
        confirmacion: '¿Iniciar esta cita?', mensaje: '✅ Cita iniciada'
    });
}

async function completarCitaPsiquiatra(id) {
    return cambiarEstadoCitaClinico({
        id, sufijo: 'Psiquiatra', url: `/api/citas/${id}/estado?estado=COMPLETADA`,
        confirmacion: '¿Marcar esta cita como completada?', mensaje: '✅ Cita completada'
    });
}

async function cancelarCitaPsiquiatra(id) {
    return cambiarEstadoCitaClinico({
        id, sufijo: 'Psiquiatra', url: `/api/citas/${id}/cancelar`,
        confirmacion: '¿Cancelar esta cita?', mensaje: '✅ Cita cancelada'
    });
}

// ==================== PSIQUIATRA - PACIENTES ====================
// ==================== PSIQUIATRA - PACIENTES ====================
// Pacientes del psiquiatra: primero asignaciones activas, luego criterio de citas.
async function obtenerPacientesPsiquiatra() {
    return obtenerPacientesClinico();
}

// Carga el módulo compartido de derivaciones (botón "Derivar" de la tabla)
(function cargarModuloDerivaciones() {
    if (document.querySelector('script[src="/js/derivaciones.js"]')) return;
    const s = document.createElement('script');
    s.src = '/js/derivaciones.js';
    document.head.appendChild(s);
})();

async function cargarPacientesPsiquiatra() {
    setLoading(true);
    try {
        const username = localStorage.getItem('username');
        const resUsuarios = await authFetch('/api/usuarios');
        let yo = null;
        if (resUsuarios.ok) {
            const usuarios = await resUsuarios.json();
            yo = usuarios.find(u => u.username === username);
        }
        
        let pacientes = await obtenerPacientesPsiquiatra();
        
        const resCitas = await authFetch('/api/citas');
        const misCitas = resCitas.ok ? (await resCitas.json()).filter(c => c.usuario?.username === username || c.usuario?.id === yo?.id) : [];
        
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
                        <button class="btn btn-sm btn-outline-secondary" onclick="abrirModalDerivar(${p.id}, '${(p.nombre || '').replace(/'/g, "\\'")}')"><i class="fas fa-share"></i> Derivar</button>
                        <button class="btn btn-sm btn-primary" onclick="verMedicamentosPaciente(${p.id}, '${(p.nombre || '').replace(/'/g, "\\'")}')">Medicamentos</button>
                        <button class="btn btn-sm btn-info" onclick="verNotasPacientePsiquiatra(${p.id}, '${(p.nombre || '').replace(/'/g, "\\'")}')">Notas</button>
                    </td>
                </tr>
            `;
        }).join('');
    } catch(e) { console.error(e); }
    setLoading(false);
}

// ==================== PSIQUIATRA - MEDICAMENTOS ====================
async function cargarPacientesMedicamento() {
    setLoading(true);
    try {
        let pacientes = await obtenerPacientesPsiquiatra();
        
        const termino = document.getElementById('buscarPacienteMedicamento')?.value || '';
        if (termino.length > 1) {
            pacientes = pacientes.filter(p => p.nombre.toLowerCase().includes(termino.toLowerCase()));
        }
        
        listaPacientesPsiquiatra = pacientes;
        
        const container = document.getElementById('listaPacientesMedicamento');
        if (pacientes.length === 0) {
            container.innerHTML = '<div class="alert alert-warning">No tienes pacientes asignados todavía</div>';
        } else {
            container.innerHTML = pacientes.map(p => `
                <div class="paciente-item" onclick="seleccionarPacienteMedicamento(${p.id}, '${(p.nombre || '').replace(/'/g, "\\'")}')">
                    <div class="paciente-avatar">${(p.nombre || 'P').charAt(0).toUpperCase()}</div>
                    <div class="paciente-nombre">
                        <strong>${p.nombre}</strong><br>
                        <small class="text-muted">${p.correo || ''} | ${p.telefono || 'Sin teléfono'}</small>
                    </div>
                </div>
            `).join('');
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
                            <div class="paciente-item" onclick="seleccionarPacienteNotaPsiquiatra(${p.id}, '${p.nombre}')">
                                <div class="paciente-avatar">${(p.nombre || 'P').charAt(0).toUpperCase()}</div>
                                <div class="paciente-nombre">
                                    <strong>${p.nombre}</strong><br>
                                    <small class="text-muted">${p.correo} | ${p.telefono || 'Sin teléfono'}</small>
                                </div>
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
    return seleccionarPacienteNotaClinico({ sufijo: 'Psiquiatra', id, nombre, alSeleccionar: (v) => { pacienteSeleccionadoIdPsiquiatra = v; } });
}

async function cargarNotasPacientePsiquiatra(pacienteId) {
    return cargarNotasPacienteClinico('Psiquiatra', pacienteId);
}

async function guardarNotaPsiquiatra() {
    return guardarNotaClinico({ sufijo: 'Psiquiatra', tiposRecomendacion: ['RECOMENDACION', 'CONSEJO', 'OBSERVACION', 'EVOLUCION'] });
}

function verNotasPacientePsiquiatra(id, nombre) {
    return verNotasPacienteClinico({ sufijo: 'Psiquiatra', id, nombre, alSeleccionar: (v) => { pacienteSeleccionadoIdPsiquiatra = v; } });
}