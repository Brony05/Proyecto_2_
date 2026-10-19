// ==================== TRABAJADOR SOCIAL - VARIABLES GLOBALES ====================
let pacienteSeleccionadoIdIntervencion = null;
let listaPacientesTS = [];

// ==================== TRABAJADOR SOCIAL - INICIALIZACIÓN ====================
document.addEventListener('DOMContentLoaded', function() {
    const sesion = verificarSesion();
    if (!sesion || sesion.rol !== 'TRABAJADOR_SOCIAL') {
        if (sesion) redirigirPorRol(sesion.rol);
        return;
    }
    
    document.getElementById('userName').innerText = localStorage.getItem('username');
    cargarModoOscuro();
    cargarDashboardTS();
    cargarCitasTS();
    cargarPacientesTS();
    cargarPacientesIntervencion();
    
    // Eventos de filtros
    document.getElementById('filtroFechaTS')?.addEventListener('change', cargarCitasTS);
    document.getElementById('filtroEstadoTS')?.addEventListener('change', cargarCitasTS);
    document.getElementById('buscarPacienteTS')?.addEventListener('input', cargarPacientesTS);
    document.getElementById('buscarPacienteIntervencion')?.addEventListener('input', cargarPacientesIntervencion);
    
    // Tabs
    document.querySelectorAll('[data-tab]').forEach(btn => {
        btn.addEventListener('click', () => {
            const tab = btn.getAttribute('data-tab');
            document.querySelectorAll('.tab-content').forEach(c => c.style.display = 'none');
            document.getElementById(tab).style.display = 'block';
            document.querySelectorAll('.nav-link').forEach(n => n.classList.remove('active'));
            btn.classList.add('active');
            
            if (tab === 'dashboard') cargarDashboardTS();
            if (tab === 'citas') cargarCitasTS();
            if (tab === 'intervenciones') cargarPacientesIntervencion();
            if (tab === 'pacientes') cargarPacientesTS();
        });
    });
});

// ==================== TRABAJADOR SOCIAL - DASHBOARD ====================
async function cargarDashboardTS() {
    setLoading(true);
    try {
        const resCitas = await authFetch('/api/citas');
        if (resCitas.ok) {
            const todasCitas = await resCitas.json();
            const misCitas = todasCitas.filter(c => c.departamento === 'TRABAJO_SOCIAL');
            
            const hoy = new Date().toISOString().split('T')[0];
            const citasHoy = misCitas.filter(c => c.fecha === hoy && c.estado !== 'CANCELADA').length;
            const citasPendientes = misCitas.filter(c => c.estado === 'AGENDADA' || c.estado === 'EN_PROGRESO').length;
            
            document.getElementById('citasHoy').innerText = citasHoy;
            document.getElementById('citasPendientes').innerText = citasPendientes;
            
            // Intervenciones realizadas (notas de tipo INTERVENCION)
            const resNotas = await authFetch('/api/notas/doctor/paciente/1').catch(() => null);
            if (resNotas && resNotas.ok) {
                const notas = await resNotas.json();
                const intervenciones = notas.filter(n => n.tipo === 'INTERVENCION');
                document.getElementById('intervencionesRealizadas').innerText = intervenciones.length;
            }
            
            // Pacientes asignados
            const pacientesIds = new Set(misCitas.map(c => c.cliente?.id));
            document.getElementById('totalPacientes').innerText = pacientesIds.size;
            
            // Próximas citas
            const proximas = misCitas.filter(c => c.fecha >= hoy && c.estado !== 'CANCELADA' && c.estado !== 'COMPLETADA').slice(0, 10);
            document.getElementById('tbodyProximasCitasTS').innerHTML = proximas.map(c => `
                <tr>
                    <td>${c.fecha || 'N/A'}</td>
                    <td>${c.hora || 'N/A'}</td>
                    <td>${c.cliente?.nombre || 'N/A'}</td>
                    <td>${c.usuario?.nombreCompleto || c.usuario?.username || 'N/A'}</td>
                    <td>${obtenerBadgeEstado(c.estado)}</td>
                    <td>
                        <button class="btn btn-sm btn-success" onclick="iniciarCitaTS(${c.id})">Iniciar</button>
                    </td>
                </tr>
            `).join('');
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

// ==================== TRABAJADOR SOCIAL - CITAS ====================
async function cargarCitasTS() {
    setLoading(true);
    try {
        const resCitas = await authFetch('/api/citas');
        if (resCitas.ok) {
            let citas = await resCitas.json();
            citas = citas.filter(c => c.departamento === 'TRABAJO_SOCIAL');
            
            const fechaFiltro = document.getElementById('filtroFechaTS')?.value;
            const estadoFiltro = document.getElementById('filtroEstadoTS')?.value;
            
            if (fechaFiltro) citas = citas.filter(c => c.fecha === fechaFiltro);
            if (estadoFiltro) citas = citas.filter(c => c.estado === estadoFiltro);
            
            document.getElementById('tbodyCitasTS').innerHTML = citas.map(c => `
                <tr>
                    <td>${c.id}</td>
                    <td>${c.fecha || 'N/A'}</td>
                    <td>${c.hora || 'N/A'}</td>
                    <td>${c.cliente?.nombre || 'N/A'}</td>
                    <td>${c.usuario?.nombreCompleto || c.usuario?.username || 'N/A'}</td>
                    <td>${c.servicio || 'N/A'}</td>
                    <td>${obtenerBadgeEstado(c.estado)}</td>
                    <td>
                        ${c.estado === 'AGENDADA' ? `<button class="btn btn-sm btn-success" onclick="iniciarCitaTS(${c.id})">Iniciar</button>` : ''}
                        ${c.estado === 'EN_PROGRESO' ? `<button class="btn btn-sm btn-warning" onclick="completarCitaTS(${c.id})">Completar</button>` : ''}
                    </td>
                </tr>
            `).join('');
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

async function iniciarCitaTS(id) {
    if (confirm('¿Iniciar esta cita?')) {
        setLoading(true);
        try {
            const res = await authFetch(`/api/citas/${id}/estado?estado=EN_PROGRESO`, { method: 'PUT' });
            if (res.ok) {
                cargarCitasTS();
                cargarDashboardTS();
                mostrarMensaje('✅ Cita iniciada');
            }
        } catch(e) { mostrarMensaje('Error', 'danger'); }
        setLoading(false);
    }
}

async function completarCitaTS(id) {
    if (confirm('¿Marcar como completada?')) {
        setLoading(true);
        try {
            const res = await authFetch(`/api/citas/${id}/estado?estado=COMPLETADA`, { method: 'PUT' });
            if (res.ok) {
                cargarCitasTS();
                cargarDashboardTS();
                mostrarMensaje('✅ Cita completada');
            }
        } catch(e) { mostrarMensaje('Error', 'danger'); }
        setLoading(false);
    }
}

// ==================== TRABAJADOR SOCIAL - PACIENTES (CORREGIDO) ====================
async function cargarPacientesTS() {
    setLoading(true);
    try {
        const resCitas = await authFetch('/api/citas');
        if (!resCitas.ok) {
            setLoading(false);
            return;
        }
        
        const todasCitas = await resCitas.json();
        const citasTS = todasCitas.filter(c => c.departamento === 'TRABAJO_SOCIAL');
        const pacientesIds = [...new Set(citasTS.map(c => c.cliente?.id))];
        
        const resPacientes = await authFetch('/api/clientes');
        if (!resPacientes.ok) {
            setLoading(false);
            return;
        }
        
        let pacientes = await resPacientes.json();
        pacientes = pacientes.filter(p => pacientesIds.includes(p.id));
        
        const termino = document.getElementById('buscarPacienteTS')?.value || '';
        if (termino.length > 1) {
            pacientes = pacientes.filter(p => p.nombre.toLowerCase().includes(termino.toLowerCase()));
        }
        
        // ================================================================
        // ⭐ CORRECCIÓN AQUÍ: Usar Promise.all para manejar async en map
        // ================================================================
        const pacientesConIntervenciones = await Promise.all(pacientes.map(async (p) => {
            try {
                const resNotas = await authFetch(`/api/notas/doctor/paciente/${p.id}`);
                let numIntervenciones = 0;
                if (resNotas && resNotas.ok) {
                    const notas = await resNotas.json();
                    numIntervenciones = notas.filter(n => n.tipo === 'INTERVENCION').length;
                }
                return { ...p, numIntervenciones };
            } catch (e) {
                return { ...p, numIntervenciones: 0 };
            }
        }));
        
        document.getElementById('tbodyPacientesTS').innerHTML = pacientesConIntervenciones.map(p => {
            const citasPaciente = citasTS.filter(c => c.cliente?.id === p.id);
            return `
                <tr>
                    <td>${p.id}</td>
                    <td>${p.nombre}</td>
                    <td>${p.telefono || 'N/A'}</td>
                    <td>${p.correo || 'N/A'}</td>
                    <td>${citasPaciente.length}</td>
                    <td><span class="badge bg-warning text-dark">${p.numIntervenciones}</span></td>
                    <td>
                        <button class="btn btn-sm btn-primary" onclick="verIntervencionesPaciente(${p.id}, '${p.nombre}')">Intervenciones</button>
                    </td>
                </tr>
            `;
        }).join('');
        
    } catch(e) { 
        console.error('Error en cargarPacientesTS:', e);
        document.getElementById('tbodyPacientesTS').innerHTML = '<tr><td colspan="7" class="text-center text-danger">Error al cargar pacientes</td></tr>';
    }
    setLoading(false);
}

// ==================== TRABAJADOR SOCIAL - INTERVENCIONES ====================
async function cargarPacientesIntervencion() {
    setLoading(true);
    try {
        const resCitas = await authFetch('/api/citas');
        if (resCitas.ok) {
            const todasCitas = await resCitas.json();
            const citasTS = todasCitas.filter(c => c.departamento === 'TRABAJO_SOCIAL');
            const pacientesIds = [...new Set(citasTS.map(c => c.cliente?.id))];
            
            const resPacientes = await authFetch('/api/clientes');
            if (resPacientes.ok) {
                let pacientes = await resPacientes.json();
                pacientes = pacientes.filter(p => pacientesIds.includes(p.id));
                listaPacientesTS = pacientes;
                
                const container = document.getElementById('listaPacientesIntervencion');
                if (pacientes.length === 0) {
                    container.innerHTML = '<div class="alert alert-warning">No tienes pacientes asignados</div>';
                } else {
                    container.innerHTML = pacientes.map(p => `
                        <div class="list-group-item list-group-item-action" 
                             onclick="seleccionarPacienteIntervencion(${p.id}, '${p.nombre}')" 
                             style="cursor: pointer; border-left: 4px solid #e65100; margin-bottom: 5px;">
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

function seleccionarPacienteIntervencion(id, nombre) {
    pacienteSeleccionadoIdIntervencion = id;
    document.getElementById('intervencionPacienteId').value = id;
    document.getElementById('pacienteSeleccionadoIntervencionNombre').innerHTML = nombre;
    document.getElementById('pacienteSeleccionadoIntervencion').style.display = 'block';
    document.getElementById('btnGuardarIntervencion').disabled = false;
    
    document.getElementById('intervencionTipo').value = '';
    document.getElementById('intervencionDescripcion').value = '';
    document.getElementById('intervencionResultado').value = '';
    
    cargarIntervencionesPaciente(id);
}

async function cargarIntervencionesPaciente(pacienteId) {
    setLoading(true);
    try {
        const res = await authFetch(`/api/notas/doctor/paciente/${pacienteId}`);
        if (res.ok) {
            const notas = await res.json();
            const intervenciones = notas.filter(n => n.tipo === 'INTERVENCION');
            const container = document.getElementById('intervencionesContainer');
            if (intervenciones.length === 0) {
                container.innerHTML = '<div class="alert alert-info">No hay intervenciones registradas para este paciente.</div>';
            } else {
                container.innerHTML = intervenciones.slice().reverse().map(i => `
                    <div class="intervencion-card">
                        <div class="d-flex justify-content-between">
                            <strong>${i.recomendaciones || i.diagnostico || 'Intervención'}</strong>
                            <small class="text-muted">${new Date(i.fechaCreacion).toLocaleString()}</small>
                        </div>
                        <p class="mt-1 mb-0">${i.medicamentos || ''}</p>
                        <small class="text-muted">Trabajador Social: ${i.doctor?.nombreCompleto || i.doctor?.username || 'Desconocido'}</small>
                    </div>
                `).join('');
            }
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

async function guardarIntervencion() {
    const pacienteId = document.getElementById('intervencionPacienteId').value;
    const tipo = document.getElementById('intervencionTipo').value;
    const descripcion = document.getElementById('intervencionDescripcion').value;
    const resultado = document.getElementById('intervencionResultado').value;
    
    if (!pacienteId) {
        mostrarMensaje('❌ Selecciona un paciente', 'danger');
        return;
    }
    if (!tipo) {
        mostrarMensaje('❌ Selecciona un tipo de intervención', 'danger');
        return;
    }
    if (!descripcion) {
        mostrarMensaje('❌ Escribe una descripción', 'danger');
        return;
    }
    
    setLoading(true);
    try {
        const contenido = `📋 INTERVENCIÓN DE TRABAJO SOCIAL\n\n` +
            `📌 Tipo: ${tipo}\n` +
            `📝 Descripción: ${descripcion}\n` +
            `📊 Resultado/Seguimiento: ${resultado || 'Pendiente'}`;
        
        const res = await authFetch('/api/notas', {
            method: 'POST',
            body: JSON.stringify({
                pacienteId: parseInt(pacienteId),
                tipo: 'INTERVENCION',
                recomendaciones: tipo,
                medicamentos: contenido
            })
        });
        
        if (res.ok) {
            document.getElementById('intervencionTipo').value = '';
            document.getElementById('intervencionDescripcion').value = '';
            document.getElementById('intervencionResultado').value = '';
            await cargarIntervencionesPaciente(parseInt(pacienteId));
            await cargarDashboardTS();
            mostrarMensaje('✅ Intervención registrada correctamente', 'success');
        } else {
            mostrarMensaje('Error al guardar', 'danger');
        }
    } catch(e) {
        mostrarMensaje('Error de conexión', 'danger');
    }
    setLoading(false);
}

function verIntervencionesPaciente(id, nombre) {
    seleccionarPacienteIntervencion(id, nombre);
    document.querySelector('[data-tab="intervenciones"]').click();
}