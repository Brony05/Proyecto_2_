// ==================== EMPLEADO - VARIABLES GLOBALES ====================

// ==================== EMPLEADO - INICIALIZACIÓN ====================
document.addEventListener('DOMContentLoaded', function() {
    const sesion = verificarSesion();
    if (!sesion || sesion.rol !== 'EMPLEADO') {
        if (sesion) redirigirPorRol(sesion.rol);
        return;
    }
    
    document.getElementById('userName').innerText = localStorage.getItem('username');
    cargarDashboard();
    cargarClientes();
    cargarCitas();
    
    // Eventos de filtros
    document.getElementById('filtroFecha')?.addEventListener('change', () => cargarCitas());
    document.getElementById('filtroEstado')?.addEventListener('change', () => cargarCitas());
    document.getElementById('filtroAsistencia')?.addEventListener('change', () => cargarCitas());
    
    // Búsqueda de clientes
    document.getElementById('buscarCliente')?.addEventListener('input', buscarCliente);
    
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
        });
    });
});

// ==================== EMPLEADO - DASHBOARD ====================
async function cargarDashboard() {
    try {
        const resClientes = await authFetch('/api/clientes');
        if (resClientes.ok) {
            const clientes = await resClientes.json();
            document.getElementById('totalClientes').innerText = clientes.length;
        }
        const resCitas = await authFetch('/api/citas');
        if (resCitas.ok) {
            const citas = await resCitas.json();
            document.getElementById('totalCitas').innerText = citas.length;
            const hoy = new Date().toISOString().split('T')[0];
            const citasHoy = citas.filter(c => c.fecha === hoy && c.estado !== 'CANCELADA').length;
            document.getElementById('citasHoy').innerText = citasHoy;
        }
    } catch(e) { console.error(e); }
}

// ==================== EMPLEADO - CLIENTES ====================
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
                    <td><button class="btn btn-sm btn-warning" onclick="editarCliente(${c.id})"><i class="fas fa-edit"></i> Editar</button></td>
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
                        <td><button class="btn btn-sm btn-warning" onclick="editarCliente(${c.id})">Editar</button></td>
                    </tr>
                `).join('');
            }
        } catch(e) {}
        setLoading(false);
    } else if (termino.length === 0) {
        cargarClientes();
    }
}

// ==================== EMPLEADO - CITAS ====================
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

// ==================== EMPLEADO - MARCAR ASISTENCIA ====================
async function marcarAsistencia(id, asistio) {
    setLoading(true);
    try {
        const res = await authFetch(`/api/citas/${id}/asistencia?asistio=${asistio}`, { method: 'PUT' });
        if (res.ok) {
            await cargarCitas();
            const mensaje = asistio ? '✅ Paciente marcado como presente' : '⚠️ Paciente marcado como ausente';
            mostrarMensaje(mensaje, asistio ? 'success' : 'warning');
        } else {
            const error = await res.text();
            mostrarMensaje('Error: ' + error, 'danger');
        }
    } catch(error) {
        console.error('Error:', error);
        mostrarMensaje('Error de conexión al marcar asistencia', 'danger');
    }
    setLoading(false);
}

async function cargarCitas() {
    setLoading(true);
    try {
        const res = await authFetch('/api/citas');
        if (res.ok) {
            let citas = await res.json();
            const estado = document.getElementById('filtroEstado')?.value;
            const fecha = document.getElementById('filtroFecha')?.value;
            if (estado) citas = citas.filter(c => c.estado === estado);
            if (fecha) citas = citas.filter(c => c.fecha === fecha);
            
            const hoy = new Date().toISOString().split('T')[0];
            const ahora = new Date().toTimeString().slice(0, 5);
            
            document.getElementById('tbodyCitas').innerHTML = citas.map(c => {
                const citaPasada = c.fecha < hoy || (c.fecha === hoy && c.hora < ahora);
                const mostrarIniciar = !citaPasada && c.estado === 'AGENDADA';
                const mostrarCompletar = !citaPasada && c.estado === 'EN_PROGRESO';
                const mostrarCancelar = !citaPasada && (c.estado === 'AGENDADA' || c.estado === 'REAGENDADA');
                
                // Badge de asistencia
                let asistenciaBadge = '';
                if (c.asistencia === 'PRESENTE') {
                    asistenciaBadge = '<span class="badge bg-success">Presente</span>';
                } else if (c.asistencia === 'AUSENTE') {
                    asistenciaBadge = '<span class="badge bg-danger">Ausente</span>';
                } else {
                    asistenciaBadge = '<span class="badge bg-secondary">Pendiente</span>';
                }
                
                return `
                    <tr>
                        <td>${c.id}</td>
                        <td>${formatearFecha(c.fecha)}</td>
                        <td>${c.hora || 'N/A'}</td>
                        <td>${c.servicio || 'N/A'}</td>
                        <td class="precio-text">${formatearPrecio(c.precio)}</td>
                        <td><i class="fas fa-user me-2"></i>${c.cliente?.nombre || 'N/A'}</td>
                        <td>${c.acompananteNombre ? c.acompananteNombre + ' (' + (c.acompananteRelacion || '-') + ')' : '-'}</td>
                        <td>${obtenerBadgeEstado(c.estado)}</td>
                        <td>${asistenciaBadge}</td>
                        <td>
                            <div class="btn-group btn-group-sm">
                                ${c.estado !== 'CANCELADA' && c.estado !== 'COMPLETADA' ? `
                                    <button class="btn btn-sm btn-success" onclick="marcarAsistencia(${c.id}, true)" title="Marcar como presente"><i class="fas fa-check-circle"></i></button>
                                    <button class="btn btn-sm btn-danger" onclick="marcarAsistencia(${c.id}, false)" title="Marcar como ausente"><i class="fas fa-times-circle"></i></button>
                                ` : '-'}
                            </div>
                        </td>
                        <td>
                            ${mostrarIniciar ? `<button class="btn btn-sm btn-warning me-1" onclick="cambiarEstado(${c.id}, 'EN_PROGRESO')"><i class="fas fa-play"></i> Iniciar</button>` : ''}
                            ${mostrarCompletar ? `<button class="btn btn-sm btn-success me-1" onclick="cambiarEstado(${c.id}, 'COMPLETADA')"><i class="fas fa-check"></i> Completar</button>` : ''}
                            ${mostrarCancelar ? `<button class="btn btn-sm btn-danger" onclick="cancelarCita(${c.id})"><i class="fas fa-times"></i> Cancelar</button>` : ''}
                        </td>
                    </tr>
                `;
            }).join('');
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

async function cambiarEstado(id, nuevoEstado) {
    let msg = nuevoEstado === 'EN_PROGRESO' ? '¿Iniciar esta terapia?' : '¿Marcar como completada?';
    if (!confirm(msg)) return;
    setLoading(true);
    try {
        const res = await authFetch(`/api/citas/${id}/estado?estado=${nuevoEstado}`, { method: 'PUT' });
        if (res.ok) {
            await cargarCitas();
            await cargarDashboard();
            mostrarMensaje(`✅ Cita ${nuevoEstado === 'EN_PROGRESO' ? 'iniciada' : 'completada'}`);
        } else {
            mostrarMensaje(await res.text(), 'danger');
        }
    } catch(error) {
        mostrarMensaje('Error', 'danger');
    }
    setLoading(false);
}

async function cancelarCita(id) {
    if (!confirm('¿Cancelar esta cita?')) return;
    setLoading(true);
    try {
        const res = await authFetch(`/api/citas/${id}/cancelar`, { method: 'PUT' });
        if (res.ok) {
            await cargarCitas();
            await cargarDashboard();
            mostrarMensaje('✅ Cita cancelada');
        } else {
            mostrarMensaje(await res.text(), 'danger');
        }
    } catch(error) {
        mostrarMensaje('Error', 'danger');
    }
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