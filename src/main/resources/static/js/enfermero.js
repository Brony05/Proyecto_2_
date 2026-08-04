// ==================== ENFERMERO - VARIABLES GLOBALES ====================
let pacienteSeleccionadoIdSignos = null;
let listaPacientesEnfermero = [];

// ==================== ENFERMERO - INICIALIZACIÓN ====================
document.addEventListener('DOMContentLoaded', function() {
    const sesion = verificarSesion();
    if (!sesion || sesion.rol !== 'ENFERMERO') {
        if (sesion) redirigirPorRol(sesion.rol);
        return;
    }
    
    document.getElementById('userName').innerText = localStorage.getItem('username');
    cargarModoOscuro();
    cargarDashboardEnfermero();
    cargarCitasEnfermero();
    cargarPacientesEnfermero();
    cargarPacientesSignos();
    
    // Eventos de filtros
    document.getElementById('filtroFechaEnfermero')?.addEventListener('change', cargarCitasEnfermero);
    document.getElementById('filtroEstadoEnfermero')?.addEventListener('change', cargarCitasEnfermero);
    document.getElementById('buscarPacienteEnfermero')?.addEventListener('input', cargarPacientesEnfermero);
    document.getElementById('buscarPacienteSignos')?.addEventListener('input', cargarPacientesSignos);
    
    // Tabs
    document.querySelectorAll('[data-tab]').forEach(btn => {
        btn.addEventListener('click', () => {
            const tab = btn.getAttribute('data-tab');
            document.querySelectorAll('.tab-content').forEach(c => c.style.display = 'none');
            document.getElementById(tab).style.display = 'block';
            document.querySelectorAll('.nav-link').forEach(n => n.classList.remove('active'));
            btn.classList.add('active');
            
            if (tab === 'dashboard') cargarDashboardEnfermero();
            if (tab === 'citas') cargarCitasEnfermero();
            if (tab === 'signos') cargarPacientesSignos();
            if (tab === 'pacientes') cargarPacientesEnfermero();
        });
    });
});

// ==================== ENFERMERO - DASHBOARD ====================
async function cargarDashboardEnfermero() {
    setLoading(true);
    try {
        const resCitas = await authFetch('/api/citas');
        if (resCitas.ok) {
            const todasCitas = await resCitas.json();
            
            // Filtrar citas asignadas al enfermero (por departamento ENFERMERIA)
            const misCitas = todasCitas.filter(c => c.departamento === 'ENFERMERIA');
            
            const hoy = new Date().toISOString().split('T')[0];
            const citasHoy = misCitas.filter(c => c.fecha === hoy && c.estado !== 'CANCELADA').length;
            const citasPendientes = misCitas.filter(c => c.estado === 'AGENDADA' || c.estado === 'EN_PROGRESO').length;
            const citasAtendidas = misCitas.filter(c => c.estado === 'COMPLETADA').length;
            
            document.getElementById('citasHoy').innerText = citasHoy;
            document.getElementById('citasPendientes').innerText = citasPendientes;
            document.getElementById('citasAtendidas').innerText = citasAtendidas;
            
            // Signos registrados (contar notas de tipo SIGNOS_VITALES)
            const resNotas = await authFetch('/api/notas/doctor/paciente/1').catch(() => null);
            if (resNotas && resNotas.ok) {
                const notas = await resNotas.json();
                const signos = notas.filter(n => n.tipo === 'SIGNOS_VITALES');
                document.getElementById('signosRegistrados').innerText = signos.length;
            }
            
            // Próximas citas
            const proximas = misCitas.filter(c => c.fecha >= hoy && c.estado !== 'CANCELADA' && c.estado !== 'COMPLETADA').slice(0, 10);
            document.getElementById('tbodyProximasCitasEnfermero').innerHTML = proximas.map(c => `
                <tr>
                    <td>${c.fecha || 'N/A'}</td>
                    <td>${c.hora || 'N/A'}</td>
                    <td>${c.cliente?.nombre || 'N/A'}</td>
                    <td>${c.usuario?.nombreCompleto || c.usuario?.username || 'N/A'}</td>
                    <td>${obtenerBadgeEstado(c.estado)}</td>
                    <td>
                        <button class="btn btn-sm btn-success" onclick="iniciarCitaEnfermero(${c.id})">Iniciar</button>
                    </td>
                </tr>
            `).join('');
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

// ==================== ENFERMERO - CITAS ====================
async function cargarCitasEnfermero() {
    setLoading(true);
    try {
        const resCitas = await authFetch('/api/citas');
        if (resCitas.ok) {
            let citas = await resCitas.json();
            citas = citas.filter(c => c.departamento === 'ENFERMERIA');
            
            const fechaFiltro = document.getElementById('filtroFechaEnfermero')?.value;
            const estadoFiltro = document.getElementById('filtroEstadoEnfermero')?.value;
            
            if (fechaFiltro) citas = citas.filter(c => c.fecha === fechaFiltro);
            if (estadoFiltro) citas = citas.filter(c => c.estado === estadoFiltro);
            
            document.getElementById('tbodyCitasEnfermero').innerHTML = citas.map(c => `
                <tr>
                    <td>${c.id}</td>
                    <td>${c.fecha || 'N/A'}</td>
                    <td>${c.hora || 'N/A'}</td>
                    <td>${c.cliente?.nombre || 'N/A'}</td>
                    <td>${c.usuario?.nombreCompleto || c.usuario?.username || 'N/A'}</td>
                    <td>${c.servicio || 'N/A'}</td>
                    <td>${obtenerBadgeEstado(c.estado)}</td>
                    <td>
                        ${c.estado === 'AGENDADA' ? `<button class="btn btn-sm btn-success" onclick="iniciarCitaEnfermero(${c.id})">Iniciar</button>` : ''}
                        ${c.estado === 'EN_PROGRESO' ? `<button class="btn btn-sm btn-warning" onclick="completarCitaEnfermero(${c.id})">Completar</button>` : ''}
                    </td>
                </tr>
            `).join('');
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

async function iniciarCitaEnfermero(id) {
    if (confirm('¿Iniciar esta cita?')) {
        setLoading(true);
        try {
            const res = await authFetch(`/api/citas/${id}/estado?estado=EN_PROGRESO`, { method: 'PUT' });
            if (res.ok) {
                cargarCitasEnfermero();
                cargarDashboardEnfermero();
                mostrarMensaje('✅ Cita iniciada');
            }
        } catch(e) { mostrarMensaje('Error', 'danger'); }
        setLoading(false);
    }
}

async function completarCitaEnfermero(id) {
    if (confirm('¿Marcar como completada?')) {
        setLoading(true);
        try {
            const res = await authFetch(`/api/citas/${id}/estado?estado=COMPLETADA`, { method: 'PUT' });
            if (res.ok) {
                cargarCitasEnfermero();
                cargarDashboardEnfermero();
                mostrarMensaje('✅ Cita completada');
            }
        } catch(e) { mostrarMensaje('Error', 'danger'); }
        setLoading(false);
    }
}

// ==================== ENFERMERO - PACIENTES ====================
async function cargarPacientesEnfermero() {
    setLoading(true);
    try {
        const resCitas = await authFetch('/api/citas');
        if (resCitas.ok) {
            const todasCitas = await resCitas.json();
            const citasEnfermeria = todasCitas.filter(c => c.departamento === 'ENFERMERIA');
            const pacientesIds = [...new Set(citasEnfermeria.map(c => c.cliente?.id))];
            
            const resPacientes = await authFetch('/api/clientes');
            if (resPacientes.ok) {
                let pacientes = await resPacientes.json();
                pacientes = pacientes.filter(p => pacientesIds.includes(p.id));
                
                const termino = document.getElementById('buscarPacienteEnfermero')?.value || '';
                if (termino.length > 1) {
                    pacientes = pacientes.filter(p => p.nombre.toLowerCase().includes(termino.toLowerCase()));
                }
                
                document.getElementById('tbodyPacientesEnfermero').innerHTML = pacientes.map(p => {
                    const citasPaciente = citasEnfermeria.filter(c => c.cliente?.id === p.id);
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
                                <button class="btn btn-sm btn-success" onclick="verSignosPaciente(${p.id}, '${p.nombre}')">Signos Vitales</button>
                            </td>
                        </tr>
                    `;
                }).join('');
            }
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

// ==================== ENFERMERO - SIGNOS VITALES ====================
async function cargarPacientesSignos() {
    setLoading(true);
    try {
        const resCitas = await authFetch('/api/citas');
        if (resCitas.ok) {
            const todasCitas = await resCitas.json();
            const citasEnfermeria = todasCitas.filter(c => c.departamento === 'ENFERMERIA');
            const pacientesIds = [...new Set(citasEnfermeria.map(c => c.cliente?.id))];
            
            const resPacientes = await authFetch('/api/clientes');
            if (resPacientes.ok) {
                let pacientes = await resPacientes.json();
                pacientes = pacientes.filter(p => pacientesIds.includes(p.id));
                listaPacientesEnfermero = pacientes;
                
                const container = document.getElementById('listaPacientesSignos');
                if (pacientes.length === 0) {
                    container.innerHTML = '<div class="alert alert-warning">No tienes pacientes asignados</div>';
                } else {
                    container.innerHTML = pacientes.map(p => `
                        <div class="list-group-item list-group-item-action" 
                             onclick="seleccionarPacienteSignos(${p.id}, '${p.nombre}')" 
                             style="cursor: pointer; border-left: 4px solid #880e4f; margin-bottom: 5px;">
                            <strong>${p.nombre}</strong><br>
                            <small class="text-muted">${p.correo} | ${p.telefono || 'Sin teléfono'}</small>
                        </div>
                    `).join('');
                }
            }
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

function seleccionarPacienteSignos(id, nombre) {
    pacienteSeleccionadoIdSignos = id;
    document.getElementById('signosPacienteId').value = id;
    document.getElementById('pacienteSeleccionadoSignosNombre').innerHTML = nombre;
    document.getElementById('pacienteSeleccionadoSignos').style.display = 'block';
    document.getElementById('btnGuardarSignos').disabled = false;
    
    document.getElementById('signosPresion').value = '';
    document.getElementById('signosCardiaca').value = '';
    document.getElementById('signosTemperatura').value = '';
    document.getElementById('signosRespiratoria').value = '';
    document.getElementById('signosObservaciones').value = '';
    
    cargarSignosVitales(id);
}

async function cargarSignosVitales(pacienteId) {
    setLoading(true);
    try {
        const res = await authFetch(`/api/notas/doctor/paciente/${pacienteId}`);
        if (res.ok) {
            const notas = await res.json();
            const signos = notas.filter(n => n.tipo === 'SIGNOS_VITALES');
            const container = document.getElementById('signosVitalesContainer');
            if (signos.length === 0) {
                container.innerHTML = '<div class="alert alert-info">No hay signos vitales registrados para este paciente.</div>';
            } else {
                container.innerHTML = signos.slice().reverse().map(s => `
                    <div class="signos-vitales-card">
                        <div class="d-flex justify-content-between">
                            <strong>${new Date(s.fechaCreacion).toLocaleString()}</strong>
                            <span class="badge bg-primary">${s.recomendaciones || 'Sin observaciones'}</span>
                        </div>
                        <div class="mt-1">
                            ${s.medicamentos || ''}
                        </div>
                        <small class="text-muted">Registrado por: ${s.doctor?.nombreCompleto || s.doctor?.username || 'Enfermero'}</small>
                    </div>
                `).join('');
            }
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

async function guardarSignosVitales() {
    const pacienteId = document.getElementById('signosPacienteId').value;
    const presion = document.getElementById('signosPresion').value;
    const cardiaca = document.getElementById('signosCardiaca').value;
    const temperatura = document.getElementById('signosTemperatura').value;
    const respiratoria = document.getElementById('signosRespiratoria').value;
    const observaciones = document.getElementById('signosObservaciones').value;
    
    if (!pacienteId) {
        mostrarMensaje('❌ Selecciona un paciente', 'danger');
        return;
    }
    if (!presion && !cardiaca && !temperatura && !respiratoria) {
        mostrarMensaje('❌ Registra al menos un signo vital', 'danger');
        return;
    }
    
    setLoading(true);
    try {
        const contenido = `📋 SIGNOS VITALES\n\n` +
            `🩸 Presión Arterial: ${presion || 'No registrada'}\n` +
            `❤️ Frecuencia Cardíaca: ${cardiaca || 'No registrada'} bpm\n` +
            `🌡️ Temperatura: ${temperatura || 'No registrada'} °C\n` +
            `💨 Frecuencia Respiratoria: ${respiratoria || 'No registrada'} rpm\n` +
            `📝 Observaciones: ${observaciones || 'Sin observaciones'}`;
        
        const res = await authFetch('/api/notas', {
            method: 'POST',
            body: JSON.stringify({
                pacienteId: parseInt(pacienteId),
                tipo: 'SIGNOS_VITALES',
                medicamentos: contenido
            })
        });
        
        if (res.ok) {
            document.getElementById('signosPresion').value = '';
            document.getElementById('signosCardiaca').value = '';
            document.getElementById('signosTemperatura').value = '';
            document.getElementById('signosRespiratoria').value = '';
            document.getElementById('signosObservaciones').value = '';
            await cargarSignosVitales(parseInt(pacienteId));
            await cargarDashboardEnfermero();
            mostrarMensaje('✅ Signos vitales registrados correctamente', 'success');
        } else {
            mostrarMensaje('Error al guardar', 'danger');
        }
    } catch(e) {
        mostrarMensaje('Error de conexión', 'danger');
    }
    setLoading(false);
}

function verSignosPaciente(id, nombre) {
    seleccionarPacienteSignos(id, nombre);
    document.querySelector('[data-tab="signos"]').click();
}