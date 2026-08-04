// ==================== DIRECTOR - VARIABLES GLOBALES ====================
let citasDepartamentoChart = null;
let personalDepartamentoChart = null;

// ==================== DIRECTOR - INICIALIZACIÓN ====================
document.addEventListener('DOMContentLoaded', function() {
    const sesion = verificarSesion();
    if (!sesion || sesion.rol !== 'DIRECTOR') {
        if (sesion) redirigirPorRol(sesion.rol);
        return;
    }
    
    document.getElementById('userName').innerText = localStorage.getItem('username');
    cargarModoOscuro();
    cargarDashboardDirector();
    cargarPersonal();
    cargarDepartamentos();
    
    // Eventos de filtros
    document.getElementById('filtroDepartamentoPersonal')?.addEventListener('change', cargarPersonal);
    document.getElementById('filtroRolPersonal')?.addEventListener('change', cargarPersonal);
    
    // Mostrar especialidad al seleccionar psicólogo o psiquiatra
    document.getElementById('nuevoRol')?.addEventListener('change', function() {
        const especialidadDiv = document.getElementById('especialidadDiv');
        especialidadDiv.style.display = (this.value === 'PSICOLOGO' || this.value === 'PSIQUIATRA') ? 'block' : 'none';
    });
    
    // Tabs
    document.querySelectorAll('[data-tab]').forEach(btn => {
        btn.addEventListener('click', () => {
            const tab = btn.getAttribute('data-tab');
            document.querySelectorAll('.tab-content').forEach(c => c.style.display = 'none');
            document.getElementById(tab).style.display = 'block';
            document.querySelectorAll('.nav-link').forEach(n => n.classList.remove('active'));
            btn.classList.add('active');
            
            if (tab === 'dashboard') cargarDashboardDirector();
            if (tab === 'departamentos') cargarDepartamentos();
            if (tab === 'personal') cargarPersonal();
        });
    });
});

// ==================== DIRECTOR - DASHBOARD ====================
async function cargarDashboardDirector() {
    setLoading(true);
    try {
        // Obtener personal
        const resPersonal = await authFetch('/api/usuarios');
        if (resPersonal.ok) {
            const personal = await resPersonal.json();
            document.getElementById('totalPersonal').innerText = personal.length;
            
            // Datos para gráfica de personal por departamento
            const deptos = {};
            personal.forEach(u => {
                const depto = u.departamento || 'SIN DEPARTAMENTO';
                deptos[depto] = (deptos[depto] || 0) + 1;
            });
            
            // Gráfica de personal por departamento
            const ctxPersonal = document.getElementById('personalDepartamentoChart').getContext('2d');
            if (personalDepartamentoChart) personalDepartamentoChart.destroy();
            personalDepartamentoChart = new Chart(ctxPersonal, {
                type: 'pie',
                data: {
                    labels: Object.keys(deptos),
                    datasets: [{
                        data: Object.values(deptos),
                        backgroundColor: ['#4caf50', '#2196f3', '#ff9800', '#9c27b0', '#795548', '#607d8b', '#f44336']
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: true,
                    plugins: { legend: { position: 'right' } }
                }
            });
        }
        
        // Obtener pacientes
        const resPacientes = await authFetch('/api/clientes');
        if (resPacientes.ok) {
            const pacientes = await resPacientes.json();
            document.getElementById('totalPacientes').innerText = pacientes.length;
        }
        
        // Obtener citas
        const resCitas = await authFetch('/api/citas');
        if (resCitas.ok) {
            const citas = await resCitas.json();
            document.getElementById('totalCitas').innerText = citas.length;
            
            // Calcular ingresos totales
            const ingresos = citas.filter(c => c.estado === 'COMPLETADA').reduce((sum, c) => sum + (c.precio || 0), 0);
            document.getElementById('ingresosTotales').innerHTML = '$' + ingresos.toLocaleString('es-MX');
            
            // Datos para gráfica de citas por departamento
            const deptosCitas = {};
            citas.forEach(c => {
                const depto = c.departamento || 'SIN DEPARTAMENTO';
                deptosCitas[depto] = (deptosCitas[depto] || 0) + 1;
            });
            
            // Gráfica de citas por departamento
            const ctxCitas = document.getElementById('citasDepartamentoChart').getContext('2d');
            if (citasDepartamentoChart) citasDepartamentoChart.destroy();
            citasDepartamentoChart = new Chart(ctxCitas, {
                type: 'bar',
                data: {
                    labels: Object.keys(deptosCitas),
                    datasets: [{
                        label: 'Citas',
                        data: Object.values(deptosCitas),
                        backgroundColor: '#1a237e',
                        borderRadius: 10
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: true,
                    plugins: { legend: { display: false } },
                    scales: { y: { beginAtZero: true } }
                }
            });
            
            // Citas recientes
            const citasRecientes = citas.slice(0, 10);
            document.getElementById('tbodyCitasRecientes').innerHTML = citasRecientes.map(c => `
                <tr>
                    <td>${c.fecha || 'N/A'}</td>
                    <td>${c.hora || 'N/A'}</td>
                    <td>${c.cliente?.nombre || 'N/A'}</td>
                    <td>${c.servicio || 'N/A'}</td>
                    <td><span class="badge-departamento ${c.departamento === 'PSICOLOGIA' ? 'badge-psicologia' : c.departamento === 'PSIQUIATRIA' ? 'badge-psiquiatria' : 'badge-administracion'}">${c.departamento || 'N/A'}</span></td>
                    <td>${obtenerBadgeEstado(c.estado)}</td>
                </tr>
            `).join('');
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

// ==================== DIRECTOR - DEPARTAMENTOS ====================
async function cargarDepartamentos() {
    setLoading(true);
    try {
        const res = await authFetch('/api/usuarios');
        if (res.ok) {
            const usuarios = await res.json();
            
            // Agrupar por departamento
            const deptos = {};
            usuarios.forEach(u => {
                const depto = u.departamento || 'SIN DEPARTAMENTO';
                if (!deptos[depto]) deptos[depto] = [];
                deptos[depto].push(u);
            });
            
            const container = document.getElementById('departamentosContainer');
            container.innerHTML = Object.keys(deptos).map(depto => `
                <div class="col-md-4">
                    <div class="stat-card">
                        <h5><i class="fas fa-building"></i> ${depto}</h5>
                        <div class="number">${deptos[depto].length}</div>
                        <div class="label">Personal</div>
                        <div class="mt-2">
                            ${deptos[depto].slice(0, 5).map(u => 
                                `<span class="badge bg-secondary me-1">${u.nombreCompleto || u.username}</span>`
                            ).join('')}
                            ${deptos[depto].length > 5 ? `<span class="badge bg-info">+${deptos[depto].length - 5} más</span>` : ''}
                        </div>
                    </div>
                </div>
            `).join('');
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

// ==================== DIRECTOR - PERSONAL ====================
async function cargarPersonal() {
    setLoading(true);
    try {
        const res = await authFetch('/api/usuarios');
        if (res.ok) {
            let usuarios = await res.json();
            
            const deptoFiltro = document.getElementById('filtroDepartamentoPersonal')?.value;
            const rolFiltro = document.getElementById('filtroRolPersonal')?.value;
            
            if (deptoFiltro) usuarios = usuarios.filter(u => u.departamento === deptoFiltro);
            if (rolFiltro) usuarios = usuarios.filter(u => u.rol === rolFiltro);
            
            document.getElementById('tbodyPersonal').innerHTML = usuarios.map(u => `
                <tr>
                    <td>${u.id}</td>
                    <td><i class="fas fa-user-circle me-2"></i>${u.nombreCompleto || u.username}</td>
                    <td>${u.username}</td>
                    <td><span class="badge ${u.rol === 'DIRECTOR' ? 'bg-danger' : u.rol.includes('JEFE') ? 'bg-warning' : 'bg-info'}">${u.rol}</span></td>
                    <td><span class="badge-departamento ${u.departamento === 'PSICOLOGIA' ? 'badge-psicologia' : u.departamento === 'PSIQUIATRIA' ? 'badge-psiquiatria' : u.departamento === 'TRABAJO_SOCIAL' ? 'badge-trabajo-social' : u.departamento === 'ENFERMERIA' ? 'badge-enfermeria' : u.departamento === 'ARCHIVO' ? 'badge-archivo' : 'badge-administracion'}">${u.departamento || 'N/A'}</span></td>
                    <td>${u.especialidad || '-'}</td>
                    <td>
                        ${u.rol !== 'DIRECTOR' ? `<button class="btn btn-sm btn-danger" onclick="eliminarUsuarioDirector(${u.id}, '${u.username}')"><i class="fas fa-trash"></i></button>` : 'Director'}
                    </td>
                </tr>
            `).join('');
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

// ==================== DIRECTOR - CREAR USUARIO ====================
function abrirModalUsuario() {
    document.getElementById('nuevoNombreCompleto').value = '';
    document.getElementById('nuevoUsername').value = '';
    document.getElementById('nuevoPassword').value = '';
    document.getElementById('nuevoTelefono').value = '';
    document.getElementById('nuevoCorreo').value = '';
    document.getElementById('nuevoRol').value = 'ADMINISTRATIVO';
    document.getElementById('nuevoDepartamento').value = '';
    document.getElementById('especialidadDiv').style.display = 'none';
    new bootstrap.Modal(document.getElementById('usuarioModal')).show();
}

async function crearUsuarioDirector() {
    const usuario = {
        nombreCompleto: document.getElementById('nuevoNombreCompleto').value,
        username: document.getElementById('nuevoUsername').value,
        password: document.getElementById('nuevoPassword').value,
        rol: document.getElementById('nuevoRol').value,
        departamento: document.getElementById('nuevoDepartamento').value,
        telefono: document.getElementById('nuevoTelefono').value,
        correo: document.getElementById('nuevoCorreo').value
    };
    
    if (!usuario.username || !usuario.password) {
        mostrarMensaje('Completa los campos obligatorios', 'danger');
        return;
    }
    if (usuario.password.length < 4) {
        mostrarMensaje('La contraseña debe tener al menos 4 caracteres', 'danger');
        return;
    }
    if (usuario.rol === 'PSICOLOGO' || usuario.rol === 'PSIQUIATRA') {
        usuario.especialidad = document.getElementById('nuevoEspecialidad').value;
        if (!usuario.especialidad) {
            mostrarMensaje('Selecciona una especialidad', 'danger');
            return;
        }
    }
    
    setLoading(true);
    try {
        const res = await authFetch('/api/usuarios', { method: 'POST', body: JSON.stringify(usuario) });
        if (res.ok) {
            bootstrap.Modal.getInstance(document.getElementById('usuarioModal')).hide();
            cargarPersonal();
            cargarDashboardDirector();
            cargarDepartamentos();
            mostrarMensaje(`✅ Usuario "${usuario.username}" creado correctamente`);
        } else {
            const error = await res.text();
            mostrarMensaje(error, 'danger');
        }
    } catch(e) {
        mostrarMensaje('Error de conexión', 'danger');
    }
    setLoading(false);
}

async function eliminarUsuarioDirector(id, username) {
    if (confirm(`¿Eliminar al usuario "${username}"?`)) {
        setLoading(true);
        try {
            const res = await authFetch(`/api/usuarios/${id}`, { method: 'DELETE' });
            if (res.ok) {
                cargarPersonal();
                cargarDashboardDirector();
                cargarDepartamentos();
                mostrarMensaje(`✅ Usuario "${username}" eliminado`);
            } else {
                mostrarMensaje('❌ Error al eliminar', 'danger');
            }
        } catch(e) {
            mostrarMensaje('Error de conexión', 'danger');
        }
        setLoading(false);
    }
}

// ==================== DIRECTOR - REPORTES ====================
async function exportarPDFDirector() {
    const token = getToken();
    if (!token) {
        mostrarMensaje('No hay sesión activa', 'danger');
        return;
    }
    
    const inicio = document.getElementById('reporteInicio').value;
    const fin = document.getElementById('reporteFin').value;
    
    if (!inicio || !fin) {
        mostrarMensaje('Selecciona un rango de fechas', 'danger');
        return;
    }
    
    setLoading(true);
    try {
        const response = await fetch(`/api/citas/exportar-pdf?inicio=${inicio}&fin=${fin}&tipo=todas`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        
        if (response.ok) {
            const blob = await response.blob();
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `reporte_director_${new Date().getTime()}.pdf`;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            URL.revokeObjectURL(url);
            mostrarMensaje('✅ PDF generado correctamente');
        } else {
            mostrarMensaje('Error al generar PDF', 'danger');
        }
    } catch (error) {
        mostrarMensaje('Error de conexión', 'danger');
    }
    setLoading(false);
}

async function realizarBackupManual() {
    setLoading(true);
    try {
        const res = await authFetch('/api/backup/manual', { method: 'POST' });
        if (res.ok) {
            mostrarMensaje('✅ Backup realizado correctamente', 'success');
        } else {
            mostrarMensaje('Error al realizar backup', 'danger');
        }
    } catch(e) {
        mostrarMensaje('Error de conexión', 'danger');
    }
    setLoading(false);
}