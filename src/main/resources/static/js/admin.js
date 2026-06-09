// ==================== ADMIN - VARIABLES GLOBALES ====================
let lineChart, doughnutChart;
let todasLasCitas = [];

// ==================== ADMIN - INICIALIZACIÓN ====================
document.addEventListener('DOMContentLoaded', function() {
    const sesion = verificarSesion();
    if (!sesion || sesion.rol !== 'ADMIN') {
        if (sesion) redirigirPorRol(sesion.rol);
        return;
    }
    
    document.getElementById('userName').innerText = localStorage.getItem('username');
    cargarModoOscuro();
    cargarDashboard();
    cargarClientes();
    cargarCitas();
    cargarUsuarios();
    cargarServicios();
    
    // Eventos de filtros
    document.getElementById('filtroFecha')?.addEventListener('change', () => cargarCitas());
    document.getElementById('filtroEstado')?.addEventListener('change', () => cargarCitas());
    
    // Búsqueda de clientes
    document.getElementById('buscarCliente')?.addEventListener('input', buscarCliente);
    
    // Mostrar especialidad al seleccionar DOCTOR
    document.getElementById('nuevoRol')?.addEventListener('change', function() {
        const especialidadDiv = document.getElementById('especialidadDiv');
        especialidadDiv.style.display = this.value === 'DOCTOR' ? 'block' : 'none';
    });
    
    // Tabs
    document.querySelectorAll('[data-tab]').forEach(btn => {
        btn.addEventListener('click', () => {
            const tab = btn.getAttribute('data-tab');
            document.querySelectorAll('.tab-content').forEach(c => c.style.display = 'none');
            document.getElementById(tab).style.display = 'block';
            document.querySelectorAll('.nav-link').forEach(n => n.classList.remove('active'));
            btn.classList.add('active');
            
            if (tab === 'dashboard') cargarDashboard();
            if (tab === 'clientes') cargarClientes();
            if (tab === 'citas') cargarCitas();
            if (tab === 'usuarios') cargarUsuarios();
            if (tab === 'servicios') cargarServicios();
        });
    });
});

// ==================== ADMIN - DASHBOARD ====================
async function cargarDashboard() {
    setLoading(true);
    try {
        const resClientes = await authFetch('/api/clientes');
        if (resClientes.ok) {
            const clientes = await resClientes.json();
            document.getElementById('totalClientes').innerText = clientes.length;
        }
        
        const resCitas = await authFetch('/api/citas');
        if (resCitas.ok) {
            todasLasCitas = await resCitas.json();
            document.getElementById('totalCitas').innerText = todasLasCitas.length;
            
            const ingresosReales = todasLasCitas
                .filter(c => c.estado === 'COMPLETADA')
                .reduce((s, c) => s + (c.precio || 0), 0);
            const ingresosProyectados = todasLasCitas
                .filter(c => c.estado === 'AGENDADA' || c.estado === 'REAGENDADA')
                .reduce((s, c) => s + (c.precio || 0), 0);
            
            document.getElementById('ingresosReales').innerHTML = formatearPrecio(ingresosReales);
            document.getElementById('ingresosProyectados').innerHTML = formatearPrecio(ingresosProyectados);
            
            // Datos para gráficas
            const dias = [];
            const citasPorDia = [];
            for (let i = 6; i >= 0; i--) {
                const fecha = new Date();
                fecha.setDate(fecha.getDate() - i);
                const fechaStr = fecha.toISOString().split('T')[0];
                dias.push(fecha.toLocaleDateString('es-MX', { weekday: 'short', day: 'numeric' }));
                citasPorDia.push(todasLasCitas.filter(c => c.fecha === fechaStr && c.estado !== 'CANCELADA').length);
            }
            
            const servicios = {};
            todasLasCitas.forEach(c => {
                const s = c.servicio || 'Consulta General';
                servicios[s] = (servicios[s] || 0) + 1;
            });
            
            // Gráfica de líneas
            const ctxLine = document.getElementById('citasLineChart').getContext('2d');
            if (lineChart) lineChart.destroy();
            lineChart = new Chart(ctxLine, {
                type: 'line',
                data: {
                    labels: dias,
                    datasets: [{
                        label: 'Citas',
                        data: citasPorDia,
                        borderColor: '#667eea',
                        backgroundColor: 'rgba(102,126,234,0.1)',
                        borderWidth: 3,
                        fill: true,
                        tension: 0.4
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: true,
                    plugins: { legend: { position: 'top' } },
                    scales: { y: { beginAtZero: true } }
                }
            });
            
            // Gráfica de doughnut
            const ctxDoughnut = document.getElementById('serviciosDoughnutChart').getContext('2d');
            if (doughnutChart) doughnutChart.destroy();
            doughnutChart = new Chart(ctxDoughnut, {
                type: 'doughnut',
                data: {
                    labels: Object.keys(servicios),
                    datasets: [{
                        data: Object.values(servicios),
                        backgroundColor: ['#667eea', '#764ba2', '#f093fb', '#f5576c', '#4facfe', '#00f2fe']
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: true,
                    plugins: { legend: { position: 'right' } }
                }
            });
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

// ==================== ADMIN - CLIENTES ====================
async function cargarClientes() {
    setLoading(true);
    try {
        const res = await authFetch('/api/clientes');
        if (res.ok) {
            const clientes = await res.json();
            document.getElementById('tbodyClientes').innerHTML = clientes.map(c => `
                <tr>
                    <td>${c.id}</td>
                    <td><i class="fas fa-user-circle text-primary me-2"></i>${c.nombre || 'N/A'}</td>
                    <td><i class="fas fa-phone me-2"></i>${c.telefono || 'N/A'}</td>
                    <td><i class="fas fa-envelope me-2"></i>${c.correo || 'N/A'}</td>
                    <td>
                        <button class="btn btn-sm btn-warning me-1" onclick="editarCliente(${c.id})"><i class="fas fa-edit"></i></button>
                        <button class="btn btn-sm btn-danger" onclick="eliminarCliente(${c.id})"><i class="fas fa-trash"></i></button>
                    </td>
                </tr>
            `).join('');
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

function abrirModalCliente() {
    document.getElementById('clienteId').value = '';
    document.getElementById('clienteNombre').value = '';
    document.getElementById('clienteTelefono').value = '';
    document.getElementById('clienteCorreo').value = '';
    new bootstrap.Modal(document.getElementById('clienteModal')).show();
}

async function guardarCliente() {
    const id = document.getElementById('clienteId').value;
    const cliente = {
        nombre: document.getElementById('clienteNombre').value,
        telefono: document.getElementById('clienteTelefono').value,
        correo: document.getElementById('clienteCorreo').value
    };
    const url = id ? `/api/clientes/${id}` : '/api/clientes';
    const method = id ? 'PUT' : 'POST';
    const res = await authFetch(url, { method, body: JSON.stringify(cliente) });
    if (res.ok) {
        bootstrap.Modal.getInstance(document.getElementById('clienteModal')).hide();
        cargarClientes();
        cargarDashboard();
        mostrarMensaje(id ? '✅ Cliente actualizado' : '✅ Cliente creado');
    }
}

async function editarCliente(id) {
    const res = await authFetch(`/api/clientes/${id}`);
    if (res.ok) {
        const c = await res.json();
        document.getElementById('clienteId').value = c.id;
        document.getElementById('clienteNombre').value = c.nombre;
        document.getElementById('clienteTelefono').value = c.telefono;
        document.getElementById('clienteCorreo').value = c.correo;
        new bootstrap.Modal(document.getElementById('clienteModal')).show();
    }
}

async function eliminarCliente(id) {
    if (confirm('¿Eliminar este cliente?')) {
        await authFetch(`/api/clientes/${id}`, { method: 'DELETE' });
        cargarClientes();
        cargarDashboard();
        mostrarMensaje('✅ Cliente eliminado');
    }
}

async function buscarCliente() {
    const termino = this.value;
    if (termino.length > 1) {
        setLoading(true);
        try {
            const res = await authFetch(`/api/clientes/buscar?nombre=${termino}`);
            if (res.ok) {
                const clientes = await res.json();
                document.getElementById('tbodyClientes').innerHTML = clientes.map(c => `
                    <tr>
                        <td>${c.id}</td>
                        <td>${c.nombre}</td>
                        <td>${c.telefono}</td>
                        <td>${c.correo}</td>
                        <td>
                            <button class="btn btn-sm btn-warning" onclick="editarCliente(${c.id})">Editar</button>
                            <button class="btn btn-sm btn-danger" onclick="eliminarCliente(${c.id})">Eliminar</button>
                        </td>
                    </tr>
                `).join('');
            }
        } catch(e) {}
        setLoading(false);
    } else if (termino.length === 0) {
        cargarClientes();
    }
}

// ==================== ADMIN - CITAS ====================
async function cargarServiciosSelect() {
    try {
        const res = await authFetch('/api/servicios');
        if (res.ok) {
            const servicios = await res.json();
            const select = document.getElementById('citaServicio');
            select.innerHTML = '<option value="">Seleccionar servicio</option>' +
                servicios.map(s => `<option value="${s.nombre}" data-precio="${s.precio}">${s.nombre} - ${formatearPrecio(s.precio)}</option>`).join('');
        }
    } catch(e) { console.error(e); }
}

function actualizarPrecio() {
    const select = document.getElementById('citaServicio');
    const selected = select.options[select.selectedIndex];
    const precio = selected.getAttribute('data-precio');
    document.getElementById('precioMostrado').innerHTML = precio ? formatearPrecio(parseInt(precio)) : '$0.00';
}

document.getElementById('citaFecha')?.addEventListener('change', async function() {
    const fecha = this.value;
    if (!fecha) return;
    setLoading(true);
    try {
        const res = await authFetch(`/api/citas/horarios-disponibles?fecha=${fecha}`);
        if (res.ok) {
            const data = await res.json();
            let horarios = data.horariosDisponibles || [];
            
            const hoy = new Date().toISOString().split('T')[0];
            const ahora = new Date();
            const horasActual = ahora.getHours();
            const minutosActual = ahora.getMinutes();
            
            if (fecha === hoy) {
                horarios = horarios.filter(h => {
                    const [hora, minuto] = h.split(':');
                    const horaNum = parseInt(hora);
                    const minutoNum = parseInt(minuto);
                    return (horaNum > horasActual) || (horaNum === horasActual && minutoNum > minutosActual);
                });
            }
            
            const horaSelect = document.getElementById('citaHora');
            if (horarios && horarios.length > 0) {
                horaSelect.innerHTML = '<option value="">Seleccionar hora</option>' + horarios.map(h => `<option value="${h}">${h}</option>`).join('');
            } else {
                horaSelect.innerHTML = '<option value="">No hay horarios disponibles</option>';
            }
        }
    } catch(e) { console.error(e); }
    setLoading(false);
});

async function cargarCitas() {
    setLoading(true);
    try {
        const res = await authFetch('/api/citas');
        if (res.ok) {
            let citas = await res.json();
            const estadoFiltro = document.getElementById('filtroEstado')?.value;
            const fechaFiltro = document.getElementById('filtroFecha')?.value;
            
            if (estadoFiltro) citas = citas.filter(c => c.estado === estadoFiltro);
            if (fechaFiltro) citas = citas.filter(c => c.fecha === fechaFiltro);
            
            document.getElementById('tbodyCitas').innerHTML = citas.map(c => `
                <tr>
                    <td>${c.id}</td>
                    <td>${formatearFecha(c.fecha)}</td>
                    <td>${c.hora || 'N/A'}</td>
                    <td><span class="badge bg-info">${c.servicio || 'N/A'}</span></td>
                    <td class="precio-text">${formatearPrecio(c.precio)}</td>
                    <td><i class="fas fa-user me-2"></i>${c.cliente?.nombre || 'N/A'}</td>
                    <td>${c.acompananteNombre ? c.acompananteNombre + ' (' + (c.acompananteRelacion || '-') + ')' : '-'}</td>
                    <td>${obtenerBadgeEstado(c.estado)}</td>
                    <td>
                        <button class="btn btn-sm btn-danger" onclick="cancelarCita(${c.id})"><i class="fas fa-times"></i> Cancelar</button>
                    </td>
                </tr>
            `).join('');
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

async function abrirModalCita() {
    await cargarServiciosSelect();
    const res = await authFetch('/api/clientes');
    if (res.ok) {
        const clientes = await res.json();
        document.getElementById('citaCliente').innerHTML = '<option value="">Seleccionar cliente</option>' +
            clientes.map(c => `<option value="${c.id}">${c.nombre}</option>`).join('');
    }
    document.getElementById('citaServicio').value = '';
    document.getElementById('citaFecha').value = '';
    document.getElementById('citaHora').innerHTML = '<option value="">Primero selecciona una fecha</option>';
    document.getElementById('precioMostrado').innerHTML = '$0.00';
    new bootstrap.Modal(document.getElementById('citaModal')).show();
}

async function guardarCita() {
    const clienteId = document.getElementById('citaCliente').value;
    const servicio = document.getElementById('citaServicio').value;
    const fecha = document.getElementById('citaFecha').value;
    const hora = document.getElementById('citaHora').value;
    
    if (!clienteId || !servicio || !fecha || !hora) {
        mostrarMensaje('Completa todos los campos', 'danger');
        return;
    }
    
    setLoading(true);
    const cita = {
        fecha, hora, servicio,
        cliente: { id: parseInt(clienteId) },
        estado: 'AGENDADA'
    };
    
    try {
        const res = await authFetch('/api/citas', { method: 'POST', body: JSON.stringify(cita) });
        if (res.ok) {
            bootstrap.Modal.getInstance(document.getElementById('citaModal')).hide();
            await cargarCitas();
            await cargarDashboard();
            mostrarMensaje('✅ Cita creada exitosamente');
        } else {
            const error = await res.text();
            mostrarMensaje('❌ ' + error, 'danger');
        }
    } catch (error) {
        mostrarMensaje('❌ Error de conexión', 'danger');
    }
    setLoading(false);
}

async function cancelarCita(id) {
    if (confirm('¿Cancelar esta cita?')) {
        await authFetch(`/api/citas/${id}/cancelar`, { method: 'PUT' });
        cargarCitas();
        cargarDashboard();
        mostrarMensaje('✅ Cita cancelada');
    }
}

// ==================== ADMIN - USUARIOS (CON NOMBRE COMPLETO) ====================
async function cargarUsuarios() {
    setLoading(true);
    try {
        const res = await authFetch('/api/usuarios');
        if (res.ok) {
            const usuarios = await res.json();
            document.getElementById('tbodyUsuarios').innerHTML = usuarios.map(u => {
                const nombreMostrar = (u.nombreCompleto && u.nombreCompleto !== '') ? u.nombreCompleto : u.username;
                return `
                    <tr>
                        <td>${u.id}</td>
                        <td><i class="fas fa-user me-2"></i>${nombreMostrar}</td>
                        <td>${u.username}</td>
                        <td><span class="badge ${u.rol === 'ADMIN' ? 'bg-danger' : u.rol === 'DOCTOR' ? 'bg-info' : 'bg-warning'}">${u.rol}</span></td>
                        <td>${u.especialidad || '-'}</td>
                        <td>
                            ${u.rol !== 'ADMIN' ? `<button class="btn btn-sm btn-danger" onclick="eliminarUsuario(${u.id}, '${u.username}')"><i class="fas fa-trash"></i> Eliminar</button>` : '<span class="text-muted">Administrador</span>'}
                        </td>
                    </tr>
                `;
            }).join('');
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

function abrirModalUsuario() {
    document.getElementById('nuevoNombreCompleto').value = '';
    document.getElementById('nuevoUsername').value = '';
    document.getElementById('nuevoPassword').value = '';
    document.getElementById('nuevoRol').value = 'EMPLEADO';
    document.getElementById('especialidadDiv').style.display = 'none';
    new bootstrap.Modal(document.getElementById('usuarioModal')).show();
}

async function crearUsuario() {
    const usuario = {
        nombreCompleto: document.getElementById('nuevoNombreCompleto').value,
        username: document.getElementById('nuevoUsername').value,
        password: document.getElementById('nuevoPassword').value,
        rol: document.getElementById('nuevoRol').value
    };
    
    if (!usuario.username || !usuario.password) {
        mostrarMensaje('Completa los campos obligatorios', 'danger');
        return;
    }
    if (usuario.password.length < 4) {
        mostrarMensaje('La contraseña debe tener al menos 4 caracteres', 'danger');
        return;
    }
    if (usuario.rol === 'DOCTOR') {
        usuario.especialidad = document.getElementById('nuevoEspecialidad').value;
        if (!usuario.especialidad) {
            mostrarMensaje('Selecciona una especialidad para el DOCTOR', 'danger');
            return;
        }
    }
    
    setLoading(true);
    try {
        const res = await authFetch('/api/usuarios', { method: 'POST', body: JSON.stringify(usuario) });
        if (res.ok) {
            bootstrap.Modal.getInstance(document.getElementById('usuarioModal')).hide();
            cargarUsuarios();
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

async function eliminarUsuario(id, username) {
    if (confirm(`¿Eliminar al usuario "${username}"?`)) {
        setLoading(true);
        const res = await authFetch(`/api/usuarios/${id}`, { method: 'DELETE' });
        if (res.ok) {
            cargarUsuarios();
            mostrarMensaje(`✅ Usuario "${username}" eliminado`);
        } else {
            mostrarMensaje('❌ Error al eliminar usuario', 'danger');
        }
        setLoading(false);
    }
}

// ==================== ADMIN - SERVICIOS ====================
async function cargarServicios() {
    setLoading(true);
    try {
        const res = await authFetch('/api/servicios/todos');
        if (res.ok) {
            const servicios = await res.json();
            document.getElementById('tbodyServicios').innerHTML = servicios.map(s => `
                <tr>
                    <td>${s.id}</td>
                    <td>${s.nombre}</td>
                    <td>${s.descripcion || '-'}</td>
                    <td class="precio-text">${formatearPrecio(s.precio)}</td>
                    <td>${s.duracionMinutos} min</td>
                    <td><span class="badge ${s.activo ? 'bg-success' : 'bg-secondary'}">${s.activo ? 'Activo' : 'Inactivo'}</span></td>
                    <td>
                        <button class="btn btn-sm btn-warning" onclick="editarServicio(${s.id})"><i class="fas fa-edit"></i> Editar</button>
                    </td>
                </tr>
            `).join('');
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

function abrirModalServicio() {
    document.getElementById('servicioId').value = '';
    document.getElementById('servicioNombre').value = '';
    document.getElementById('servicioDescripcion').value = '';
    document.getElementById('servicioPrecio').value = '';
    document.getElementById('servicioDuracion').value = '';
    document.getElementById('servicioActivo').value = 'true';
    new bootstrap.Modal(document.getElementById('servicioModal')).show();
}

async function editarServicio(id) {
    const res = await authFetch(`/api/servicios/${id}`);
    if (res.ok) {
        const s = await res.json();
        document.getElementById('servicioId').value = s.id;
        document.getElementById('servicioNombre').value = s.nombre;
        document.getElementById('servicioDescripcion').value = s.descripcion || '';
        document.getElementById('servicioPrecio').value = s.precio;
        document.getElementById('servicioDuracion').value = s.duracionMinutos;
        document.getElementById('servicioActivo').value = s.activo;
        new bootstrap.Modal(document.getElementById('servicioModal')).show();
    }
}

async function guardarServicio() {
    const id = document.getElementById('servicioId').value;
    const servicio = {
        nombre: document.getElementById('servicioNombre').value,
        descripcion: document.getElementById('servicioDescripcion').value,
        precio: parseFloat(document.getElementById('servicioPrecio').value),
        duracionMinutos: parseInt(document.getElementById('servicioDuracion').value),
        activo: document.getElementById('servicioActivo').value === 'true'
    };
    
    const url = id ? `/api/servicios/${id}` : '/api/servicios';
    const method = id ? 'PUT' : 'POST';
    const res = await authFetch(url, { method, body: JSON.stringify(servicio) });
    if (res.ok) {
        bootstrap.Modal.getInstance(document.getElementById('servicioModal')).hide();
        cargarServicios();
        mostrarMensaje(id ? '✅ Servicio actualizado' : '✅ Servicio creado');
    }
}

// ==================== ADMIN - REPORTES ====================
async function exportarPDF() {
    const token = getToken();
    if (!token) {
        mostrarMensaje('No hay sesión activa', 'danger');
        return;
    }
    
    const inicio = document.getElementById('reporteInicio').value;
    const fin = document.getElementById('reporteFin').value;
    const tipo = document.getElementById('reporteTipo').value;
    
    if (!inicio || !fin) {
        mostrarMensaje('Selecciona un rango de fechas', 'danger');
        return;
    }
    
    setLoading(true);
    try {
        const response = await fetch(`/api/citas/exportar-pdf?inicio=${inicio}&fin=${fin}&tipo=${tipo}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        
        if (response.ok) {
            const blob = await response.blob();
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `reporte_citas_${new Date().getTime()}.pdf`;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            URL.revokeObjectURL(url);
            mostrarMensaje('✅ PDF generado correctamente');
        } else if (response.status === 401) {
            mostrarMensaje('Sesión expirada', 'danger');
            setTimeout(() => { window.location.href = '/'; }, 2000);
        } else {
            const error = await response.text();
            mostrarMensaje('Error: ' + error, 'danger');
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