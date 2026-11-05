// ==================== JEFE - VARIABLES GLOBALES ====================
let departamentoActual = '';
let miEquipo = [];
let asigProfesionalSeleccionado = null;
let asigPacienteSeleccionado = null;

// ==================== JEFE - INICIALIZACIÓN ====================
document.addEventListener('DOMContentLoaded', function() {
    const sesion = verificarSesion();
    if (!sesion) return;
    
    const rolesPermitidos = ['JEFE_PSICOLOGIA', 'JEFE_PSIQUIATRIA', 'JEFE_TRABAJO_SOCIAL', 'JEFE_ENFERMERIA', 'JEFE_ARCHIVO'];
    
    if (!rolesPermitidos.includes(sesion.rol)) {
        if (sesion.rol === 'DIRECTOR') window.location.href = '/director.html';
        else if (sesion.rol === 'ADMIN') window.location.href = '/admin.html';
        else redirigirPorRol(sesion.rol);
        return;
    }
    
    // El Jefe de Archivo tiene su módulo propio de archivo clínico
    if (sesion.rol === 'JEFE_ARCHIVO') {
        window.location.href = '/jefe-archivo.html';
        return;
    }
    
    document.getElementById('userName').innerText = localStorage.getItem('username');
    const avatarJefe = document.getElementById('clinAvatar');
    if (avatarJefe) avatarJefe.innerText = (localStorage.getItem('username') || 'J').charAt(0).toUpperCase();
    cargarModoOscuro();
    
    // Obtener departamento del jefe
    obtenerDepartamentoJefe();
    
    // Tabs
    document.querySelectorAll('[data-tab]').forEach(btn => {
        btn.addEventListener('click', () => {
            const tab = btn.getAttribute('data-tab');
            document.querySelectorAll('.tab-content').forEach(c => c.style.display = 'none');
            document.getElementById(tab).style.display = 'block';
            document.querySelectorAll('.nav-link').forEach(n => n.classList.remove('active'));
            btn.classList.add('active');
            
            if (tab === 'dashboard') cargarDashboardJefe();
            if (tab === 'equipo') cargarEquipo();
            if (tab === 'asignaciones') iniciarTabAsignaciones();
            if (tab === 'citas') cargarCitasJefe();
            if (tab === 'pacientes') cargarPacientesJefe();
        });
    });
    
    // Eventos de asignaciones
    document.getElementById('asigProfesional')?.addEventListener('change', seleccionarProfesionalAsig);
    document.getElementById('asigBuscarPaciente')?.addEventListener('input', cargarListadoPacientesAsig);
    
    // Eventos de filtros
    document.getElementById('filtroFechaJefe')?.addEventListener('change', cargarCitasJefe);
    document.getElementById('filtroEstadoJefe')?.addEventListener('change', cargarCitasJefe);
    document.getElementById('buscarPacienteJefe')?.addEventListener('input', cargarPacientesJefe);
});

// ==================== JEFE - OBTENER DEPARTAMENTO ====================
async function obtenerDepartamentoJefe() {
    try {
        const username = localStorage.getItem('username');
        const res = await authFetch('/api/usuarios');
        if (res.ok) {
            const usuarios = await res.json();
            const usuario = usuarios.find(u => u.username === username);
            if (usuario) {
                departamentoActual = usuario.departamento || 'DEPARTAMENTO';
                const nombres = {
                    'PSICOLOGIA': 'Psicología',
                    'PSIQUIATRIA': 'Psiquiatría',
                    'TRABAJO_SOCIAL': 'Trabajo Social',
                    'ENFERMERIA': 'Enfermería',
                    'ARCHIVO': 'Archivo Clínico',
                    'ADMINISTRACION': 'Administración'
                };
                document.getElementById('departamentoNombre').innerText = 'Depto. ' + (nombres[departamentoActual] || departamentoActual);
                const badgeDepto = document.getElementById('departamentoBadge');
                if (badgeDepto) badgeDepto.innerText = nombres[departamentoActual] || departamentoActual;
            }
        }
    } catch(e) { console.error(e); }
}

// ==================== JEFE - DASHBOARD ====================
async function cargarDashboardJefe() {
    setLoading(true);
    try {
        // Obtener mi equipo
        const resUsuarios = await authFetch('/api/usuarios');
        if (resUsuarios.ok) {
            const usuarios = await resUsuarios.json();
            miEquipo = usuarios.filter(u => u.departamento === departamentoActual && u.rol !== 'DIRECTOR');
            document.getElementById('totalEquipo').innerText = miEquipo.length;
            
            // Resumen de equipo
            const roles = {};
            miEquipo.forEach(u => {
                roles[u.rol] = (roles[u.rol] || 0) + 1;
            });
            
            document.getElementById('tbodyEquipoResumen').innerHTML = miEquipo.slice(0, 5).map(u => `
                <tr>
                    <td>${u.nombreCompleto || u.username}</td>
                    <td><span class="badge bg-info">${u.rol}</span></td>
                    <td>${u.especialidad || '-'}</td>
                </tr>
            `).join('');
        }
        
        // Obtener citas del departamento
        const resCitas = await authFetch('/api/citas');
        if (resCitas.ok) {
            const todasCitas = await resCitas.json();
            const hoy = new Date().toISOString().split('T')[0];
            const semanaAtras = new Date();
            semanaAtras.setDate(semanaAtras.getDate() - 7);
            const semanaStr = semanaAtras.toISOString().split('T')[0];
            
            // Filtrar citas del departamento
            const citasDepto = todasCitas.filter(c => c.departamento === departamentoActual);
            
            const citasHoy = citasDepto.filter(c => c.fecha === hoy && c.estado !== 'CANCELADA').length;
            const citasSemana = citasDepto.filter(c => c.fecha >= semanaStr && c.estado !== 'CANCELADA').length;
            const citasCompletadas = citasDepto.filter(c => c.estado === 'COMPLETADA').length;
            
            document.getElementById('citasHoy').innerText = citasHoy;
            document.getElementById('citasSemana').innerText = citasSemana;
            document.getElementById('citasCompletadas').innerText = citasCompletadas;
            
            // Próximas citas
            const proximas = citasDepto.filter(c => c.fecha >= hoy && c.estado !== 'CANCELADA' && c.estado !== 'COMPLETADA').slice(0, 10);
            document.getElementById('tbodyProximasCitasJefe').innerHTML = proximas.map(c => `
                <tr>
                    <td>${c.fecha || 'N/A'}</td>
                    <td>${c.hora || 'N/A'}</td>
                    <td>${c.cliente?.nombre || 'N/A'}</td>
                    <td>${c.usuario?.nombreCompleto || c.usuario?.username || 'N/A'}</td>
                    <td>${obtenerBadgeEstado(c.estado)}</td>
                </tr>
            `).join('');
            
            // Gráficas del departamento
            await dibujarGraficasClinicas(citasDepto);
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

// ==================== JEFE - EQUIPO ====================
async function cargarEquipo() {
    setLoading(true);
    try {
        const res = await authFetch('/api/usuarios');
        if (res.ok) {
            const usuarios = await res.json();
            const equipo = usuarios.filter(u => u.departamento === departamentoActual && u.rol !== 'DIRECTOR');
            
            const nombresDepto = {
                'PSICOLOGIA': 'badge-psicologia',
                'PSIQUIATRIA': 'badge-psiquiatria',
                'TRABAJO_SOCIAL': 'badge-trabajo-social',
                'ENFERMERIA': 'badge-enfermeria',
                'ARCHIVO': 'badge-archivo',
                'ADMINISTRACION': 'badge-administracion'
            };
            const container = document.getElementById('equipoContainer');
            container.innerHTML = equipo.map(u => `
                <div class="col-md-4">
                    <div class="miembro-card">
                        <div class="miembro-top">
                            <div class="paciente-avatar">${(u.nombreCompleto || u.username || '?').charAt(0).toUpperCase()}</div>
                            <div class="flex-grow-1">
                                <h6 class="mb-1 fw-bold">${u.nombreCompleto || u.username}</h6>
                                <span class="badge bg-info">${u.rol}</span>
                                ${u.especialidad ? `<span class="badge bg-white text-dark border">${u.especialidad}</span>` : ''}
                            </div>
                            <div>
                                <span class="badge-departamento ${nombresDepto[departamentoActual] || 'badge-administracion'}">${departamentoActual}</span>
                            </div>
                        </div>
                        <div class="mt-2 small text-muted miembro-datos">
                            <i class="fas fa-envelope"></i> ${u.correo || 'Sin correo'} &nbsp;·&nbsp;
                            <i class="fas fa-phone"></i> ${u.telefono || 'Sin teléfono'}
                        </div>
                    </div>
                </div>
            `).join('');
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

// ==================== JEFE - CITAS ====================
async function cargarCitasJefe() {
    setLoading(true);
    try {
        const res = await authFetch('/api/citas');
        if (res.ok) {
            let citas = await res.json();
            
            // Filtrar por departamento
            citas = citas.filter(c => c.departamento === departamentoActual);
            
            const fechaFiltro = document.getElementById('filtroFechaJefe')?.value;
            const estadoFiltro = document.getElementById('filtroEstadoJefe')?.value;
            
            if (fechaFiltro) citas = citas.filter(c => c.fecha === fechaFiltro);
            if (estadoFiltro) citas = citas.filter(c => c.estado === estadoFiltro);
            
            document.getElementById('tbodyCitasJefe').innerHTML = citas.map(c => `
                <tr>
                    <td>${c.id}</td>
                    <td>${c.fecha || 'N/A'}</td>
                    <td>${c.hora || 'N/A'}</td>
                    <td>${c.cliente?.nombre || 'N/A'}</td>
                    <td>${c.servicio || 'N/A'}</td>
                    <td>${c.usuario?.nombreCompleto || c.usuario?.username || 'N/A'}</td>
                    <td>${obtenerBadgeEstado(c.estado)}</td>
                    <td>
                        ${c.estado !== 'CANCELADA' && c.estado !== 'COMPLETADA' ? 
                            `<button class="btn btn-sm btn-danger" onclick="cancelarCitaJefe(${c.id})">Cancelar</button>` : ''}
                    </td>
                </tr>
            `).join('');
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

async function cancelarCitaJefe(id) {
    if (confirm('¿Cancelar esta cita?')) {
        setLoading(true);
        try {
            const res = await authFetch(`/api/citas/${id}/cancelar`, { method: 'PUT' });
            if (res.ok) {
                cargarCitasJefe();
                cargarDashboardJefe();
                mostrarMensaje('✅ Cita cancelada');
            } else {
                mostrarMensaje('Error al cancelar', 'danger');
            }
        } catch(e) {
            mostrarMensaje('Error de conexión', 'danger');
        }
        setLoading(false);
    }
}

// ==================== JEFE - ASIGNACIONES ====================
async function iniciarTabAsignaciones() {
    await cargarProfesionalesAsig();
    await cargarListadoPacientesAsig();
    await cargarAsignacionesEquipo();
}

async function cargarProfesionalesAsig() {
    try {
        const res = await authFetch('/api/usuarios');
        if (!res.ok) return;
        const usuarios = await res.json();
        // Mi equipo: mismo departamento, rol clínico (no el director, ni jefes)
        const equipo = usuarios.filter(u => u.departamento === departamentoActual
            && ['DOCTOR', 'PSICOLOGO', 'PSIQUIATRA', 'ENFERMERO', 'TRABAJADOR_SOCIAL'].includes(u.rol));
        miEquipo = equipo;
        const select = document.getElementById('asigProfesional');
        select.innerHTML = '<option value="">Seleccionar profesional</option>' +
            equipo.map(u => `<option value="${u.id}">${u.nombreCompleto || u.username} (${u.especialidad || u.rol})</option>`).join('');
    } catch(e) { console.error(e); }
}

function seleccionarProfesionalAsig() {
    const valor = document.getElementById('asigProfesional').value;
    asigProfesionalSeleccionado = valor ? parseInt(valor, 10) : null;
    actualizarBotonAsig();
}

async function cargarListadoPacientesAsig() {
    try {
        const termino = document.getElementById('asigBuscarPaciente')?.value || '';
        let url = '/api/clientes';
        if (termino.length > 1) url += `/buscar?nombre=${encodeURIComponent(termino)}`;
        const res = await authFetch(url);
        if (!res.ok) return;
        const pacientes = (await res.json()).slice(0, 40);
        const container = document.getElementById('asigListadoPacientes');
        container.innerHTML = pacientes.map(p => `
            <div class="paciente-item ${asigPacienteSeleccionado === p.id ? 'selected' : ''}"
                 onclick="seleccionarPacienteAsig(${p.id}, '${(p.nombre || '').replace(/'/g, "\\'")}', this)">
                <div class="paciente-avatar">${(p.nombre || 'P').charAt(0).toUpperCase()}</div>
                <div class="paciente-nombre">
                    <strong>${p.nombre}</strong><br>
                    <small class="text-muted">${p.correo || ''}</small>
                </div>
            </div>
        `).join('');
    } catch(e) { console.error(e); }
}

function seleccionarPacienteAsig(id, nombre, el) {
    asigPacienteSeleccionado = id;
    document.getElementById('btnAsignarPaciente').dataset.nombrePaciente = nombre;
    document.querySelectorAll('#asigListadoPacientes .paciente-item').forEach(item => item.classList.remove('selected'));
    if (el) el.classList.add('selected');
    actualizarBotonAsig();
}

function actualizarBotonAsig() {
    const btn = document.getElementById('btnAsignarPaciente');
    btn.disabled = !(asigProfesionalSeleccionado && asigPacienteSeleccionado);
}

async function crearAsignacion() {
    if (!asigProfesionalSeleccionado || !asigPacienteSeleccionado) return;
    setLoading(true);
    try {
        const res = await authFetch('/api/asignaciones', {
            method: 'POST',
            body: JSON.stringify({
                clienteId: asigPacienteSeleccionado,
                usuarioId: asigProfesionalSeleccionado,
                motivo: document.getElementById('asigMotivo').value || 'Asignación por jefatura'
            })
        });
        if (res.ok) {
            mostrarMensaje('✅ Paciente asignado correctamente');
            seleccionarProfesionalAsig();
            asigPacienteSeleccionado = null;
            document.getElementById('asigMotivo').value = '';
            actualizarBotonAsig();
            await cargarListadoPacientesAsig();
            await cargarAsignacionesEquipo();
        } else {
            const texto = await res.text();
            mostrarMensaje('❌ ' + (texto.slice(0, 140) || 'Error al asignar'), 'danger');
        }
    } catch(e) {
        mostrarMensaje('Error de conexión', 'danger');
    }
    setLoading(false);
}

async function cargarAsignacionesEquipo() {
    try {
        const res = await authFetch('/api/usuarios');
        if (!res.ok) return;
        const usuarios = await res.json();
        const equipo = usuarios.filter(u => u.departamento === departamentoActual
            && ['DOCTOR', 'PSICOLOGO', 'PSIQUIATRA', 'ENFERMERO', 'TRABAJADOR_SOCIAL'].includes(u.rol));
        
        const filas = [];
        for (const miembro of equipo) {
            const resAsig = await authFetch(`/api/asignaciones/persona/${miembro.id}`);
            if (!resAsig.ok) continue;
            const asigs = await resAsig.json();
            asigs.forEach(a => filas.push({ asig: a, miembro }));
        }
        
        document.getElementById('tbodyAsignacionesEquipo').innerHTML = filas.length === 0
            ? '<tr><td colspan="5" class="text-center text-muted">Sin asignaciones activas todavía</td></tr>'
            : filas.map(({asig, miembro}) => `
                <tr>
                    <td><strong>${asig.cliente?.nombre || asig.clienteId || 'N/A'}</strong></td>
                    <td>${miembro.nombreCompleto || miembro.username}</td>
                    <td>${asig.motivo || '-'}</td>
                    <td>${asig.fechaInicio || '-'}</td>
                    <td><button class="btn btn-sm btn-danger" onclick="liberarAsignacion(${asig.id})"><i class="fas fa-unlink"></i> Liberar</button></td>
                </tr>
            `).join('');
    } catch(e) { console.error(e); }
}

async function liberarAsignacion(id) {
    if (!confirm('¿Liberar esta asignación? El profesional dejará de ver al paciente.')) return;
    setLoading(true);
    try {
        const res = await authFetch(`/api/asignaciones/${id}`, { method: 'DELETE' });
        if (res.ok) {
            mostrarMensaje('✅ Asignación liberada');
            await cargarAsignacionesEquipo();
        } else {
            mostrarMensaje('Error al liberar', 'danger');
        }
    } catch(e) {
        mostrarMensaje('Error de conexión', 'danger');
    }
    setLoading(false);
}

// ==================== JEFE - PACIENTES ====================
async function cargarPacientesJefe() {
    setLoading(true);
    try {
        const termino = document.getElementById('buscarPacienteJefe')?.value || '';
        
        let url = '/api/clientes';
        if (termino.length > 1) {
            url += `/buscar?nombre=${termino}`;
        }
        
        const res = await authFetch(url);
        if (res.ok) {
            const pacientes = await res.json();
            
            // Obtener citas para contar
            const resCitas = await authFetch('/api/citas');
            const todasCitas = await resCitas.json();
            const citasDepto = todasCitas.filter(c => c.departamento === departamentoActual);
            
            document.getElementById('tbodyPacientesJefe').innerHTML = pacientes.map(p => {
                const citasPaciente = citasDepto.filter(c => c.cliente?.id === p.id);
                const ultimaCita = citasPaciente.sort((a,b) => new Date(b.fecha) - new Date(a.fecha))[0];
                return `
                    <tr>
                        <td>${p.id}</td>
                        <td>${p.nombre}</td>
                        <td>${p.telefono || 'N/A'}</td>
                        <td>${p.correo || 'N/A'}</td>
                        <td>${citasPaciente.length}</td>
                        <td>${ultimaCita ? ultimaCita.fecha + ' ' + ultimaCita.hora : '-'}</td>
                    </tr>
                `;
            }).join('');
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}