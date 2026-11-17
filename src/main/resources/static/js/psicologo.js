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
    const avatarClin = document.getElementById('clinAvatar');
    if (avatarClin) avatarClin.innerText = (localStorage.getItem('username') || 'P').charAt(0).toUpperCase();
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
    return cargarDashboardClinico('Psicologo');
}

// ==================== PSICÓLOGO - CITAS ====================
async function cargarCitasPsicologo() {
    return cargarCitasClinico('Psicologo');
}

async function iniciarCitaPsicologo(id) {
    return cambiarEstadoCitaClinico({
        id, sufijo: 'Psicologo', url: `/api/citas/${id}/estado?estado=EN_PROGRESO`,
        confirmacion: '¿Iniciar esta cita?', mensaje: '✅ Cita iniciada'
    });
}

async function completarCitaPsicologo(id) {
    return cambiarEstadoCitaClinico({
        id, sufijo: 'Psicologo', url: `/api/citas/${id}/estado?estado=COMPLETADA`,
        confirmacion: '¿Marcar esta cita como completada?', mensaje: '✅ Cita completada'
    });
}

async function cancelarCitaPsicologo(id) {
    return cambiarEstadoCitaClinico({
        id, sufijo: 'Psicologo', url: `/api/citas/${id}/cancelar`,
        confirmacion: '¿Cancelar esta cita?', mensaje: '✅ Cita cancelada'
    });
}

// ==================== PSICÓLOGO - PACIENTES ====================
// ==================== PSICÓLOGO - PACIENTES ====================
// Los pacientes del psicólogo salen de sus ASIGNACIONES activas; si la
// clínica todavía los rastrea solo por citas, cae al criterio anterior.
async function obtenerPacientesPsicologo() {
    return obtenerPacientesClinico();
}

// Carga el módulo compartido de derivaciones (botón "Derivar" de la tabla)
(function cargarModuloDerivaciones() {
    if (document.querySelector('script[src="/js/derivaciones.js"]')) return;
    const s = document.createElement('script');
    s.src = '/js/derivaciones.js';
    document.head.appendChild(s);
})();

async function cargarPacientesPsicologo() {
    setLoading(true);
    try {
        const username = localStorage.getItem('username');
        const resUsuarios = await authFetch('/api/usuarios');
        let yo = null;
        if (resUsuarios.ok) {
            const usuarios = await resUsuarios.json();
            yo = usuarios.find(u => u.username === username);
        }
        
        let pacientes = await obtenerPacientesPsicologo();
        
        const resCitas = await authFetch('/api/citas');
        const misCitas = resCitas.ok ? (await resCitas.json()).filter(c => c.usuario?.username === username || c.usuario?.id === yo?.id) : [];
        
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
                        <button class="btn btn-sm btn-outline-secondary" onclick="abrirModalDerivar(${p.id}, '${(p.nombre || '').replace(/'/g, "\\'")}')"><i class="fas fa-share"></i> Derivar</button>
                        <button class="btn btn-sm btn-info" onclick="verNotasPacientePsicologo(${p.id}, '${(p.nombre || '').replace(/'/g, "\\'")}')">Ver notas</button>
                    </td>
                </tr>
            `;
        }).join('');
    } catch(e) { console.error(e); }
    setLoading(false);
}

// ==================== PSICÓLOGO - NOTAS CLÍNICAS ====================
async function cargarPacientesNotaPsicologo() {
    setLoading(true);
    try {
        const pacientes = await obtenerPacientesPsicologo();
        
        const container = document.getElementById('listaPacientesNotaPsicologo');
        if (pacientes.length === 0) {
            container.innerHTML = '<div class="alert alert-warning">No tienes pacientes asignados todavía</div>';
        } else {
            container.innerHTML = pacientes.map(p => `
                <div class="paciente-item" onclick="seleccionarPacienteNotaPsicologo(${p.id}, '${(p.nombre || '').replace(/'/g, "\\'")}')">
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

function seleccionarPacienteNotaPsicologo(id, nombre) {
    return seleccionarPacienteNotaClinico({ sufijo: 'Psicologo', id, nombre, alSeleccionar: (v) => { pacienteSeleccionadoIdPsicologo = v; } });
}

async function cargarNotasPacientePsicologo(pacienteId) {
    return cargarNotasPacienteClinico('Psicologo', pacienteId);
}

async function guardarNotaPsicologo() {
    return guardarNotaClinico({ sufijo: 'Psicologo' });
}

function verNotasPacientePsicologo(id, nombre) {
    return verNotasPacienteClinico({ sufijo: 'Psicologo', id, nombre, alSeleccionar: (v) => { pacienteSeleccionadoIdPsicologo = v; } });
}