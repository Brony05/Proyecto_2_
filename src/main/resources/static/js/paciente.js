// ==================== PACIENTE - VARIABLES GLOBALES ====================
let servicioActual = null, precioActual = null, fechaActual = null, horaActual = null;
let horaReagendarSeleccionada = null;

/** Divide 'YYYY-MM-DD' en { dia, mes corto, largo } para las tarjetas de cita. */
function partesFecha(fecha) {
    const meses = ['ENE', 'FEB', 'MAR', 'ABR', 'MAY', 'JUN', 'JUL', 'AGO', 'SEP', 'OCT', 'NOV', 'DIC'];
    const p = String(fecha || '').split('-');
    const m = parseInt(p[1]);
    return { dia: p[2] || '?', mes: (m >= 1 && m <= 12) ? meses[m - 1] : '', largo: formatearFecha(fecha) };
}

// ==================== PACIENTE - INICIALIZACIÓN ====================
document.addEventListener('DOMContentLoaded', function() {
    const sesion = verificarSesion();
    if (!sesion || sesion.rol !== 'PACIENTE') {
        if (sesion) redirigirPorRol(sesion.rol);
        return;
    }
    
    const nombre = localStorage.getItem('username') || 'Paciente';
    document.getElementById('userName').innerText = nombre;
    document.getElementById('avatarInicial').innerText = nombre.charAt(0).toUpperCase();
    document.getElementById('heroSaludo').innerText = '\u00A1Hola, ' + nombre + '! \uD83D\uDC4B';
    document.getElementById('heroFecha').innerText = new Date().toLocaleDateString('es-MX',
        { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
    cargarCitas();
    cargarMensajesNoLeidos();
    
    // Tabs
    document.querySelectorAll('[data-tab]').forEach(btn => {
        btn.addEventListener('click', () => {
            const tab = btn.getAttribute('data-tab');
            document.querySelectorAll('.tab-content').forEach(c => c.style.display = 'none');
            document.getElementById(tab).style.display = 'block';
            document.querySelectorAll('.nav-link').forEach(n => n.classList.remove('active'));
            btn.classList.add('active');
            
            if (tab === 'citas') cargarCitas();
            if (tab === 'historial') cargarHistoriaClinica();
            if (tab === 'listaEspera') cargarMisSolicitudes();
            if (tab === 'mensajes') { cargarMensajes(); cargarMensajesNoLeidos(); }
        });
    });
    
    setInterval(cargarMensajesNoLeidos, 30000);
});

// ==================== PACIENTE - MENSAJES NO LEÍDOS ====================
async function cargarMensajesNoLeidos() {
    try {
        const res = await authFetch('/api/mensajes/no-leidos/count');
        if (res.ok) {
            const data = await res.json();
            const badge = document.getElementById('mensajesNoLeidos');
            if (data.count > 0) {
                badge.style.display = 'flex';
                badge.innerText = data.count > 9 ? '9+' : data.count;
            } else {
                badge.style.display = 'none';
            }
        }
    } catch(e) { console.error(e); }
}

// ==================== PACIENTE - SERVICIOS ====================
let agendando = false; // Evita doble click / doble envío al agendar

async function cargarServicios() {
    try {
        const citasRes = await authFetch('/api/citas');
        const citas = await citasRes.json();
        // Consulta Inicial activa O completada => ya no se puede volver a agendar
        const tuvoConsultaInicial = citas.some(c => c.servicio === 'Consulta Inicial' && c.estado !== 'CANCELADA');
        
        const res = await authFetch('/api/servicios');
        if (res.ok) {
            const servicios = await res.json();
            const serviciosFiltrados = tuvoConsultaInicial ? servicios.filter(s => s.nombre !== 'Consulta Inicial') : servicios;
            
            document.getElementById('serviciosContainer').innerHTML = serviciosFiltrados.map(s => `
                <div class="col-md-6"><div class="servicio-card p-3" data-servicio="${s.nombre}" data-precio="${s.precio}">
                    <i class="fas fa-stethoscope text-primary"></i> <strong>${s.nombre}</strong>
                    <span class="float-end precio-text">${formatearPrecio(s.precio)}</span>
                    <small class="d-block text-muted">${s.descripcion || ''} - ${s.duracionMinutos} min</small>
                </div></div>
            `).join('');
            
            if (tuvoConsultaInicial) {
                document.getElementById('serviciosContainer').insertAdjacentHTML('afterbegin', 
                    `<div class="alert alert-info mb-3"><i class="fas fa-info-circle"></i> Ya tienes tu Consulta Inicial. Ahora puedes agendar citas de seguimiento con tu especialista.</div>`);
            }
            
            document.querySelectorAll('.servicio-card').forEach(card => {
                card.addEventListener('click', function() {
                    document.querySelectorAll('.servicio-card').forEach(c => c.classList.remove('selected'));
                    this.classList.add('selected');
                    servicioActual = this.getAttribute('data-servicio');
                    precioActual = parseFloat(this.getAttribute('data-precio'));
                    document.getElementById('precioMostrado').innerHTML = formatearPrecio(precioActual);
                    document.getElementById('acompananteSection').style.display = (servicioActual === 'Terapia de Pareja' || servicioActual === 'Terapia Familiar') ? 'block' : 'none';
                    verificarFormularioCompleto();
                });
            });
        }
    } catch(e) { console.error(e); }
}

// ==================== PACIENTE - HORARIOS ====================
document.getElementById('citaFecha')?.addEventListener('change', async function() {
    const fecha = this.value;
    if (!fecha) return;
    fechaActual = fecha;
    setLoading(true);
    try {
        // Pregunta la agenda del especialista que atendería el servicio elegido
        const servicioQ = servicioActual ? `&servicio=${encodeURIComponent(servicioActual)}` : '';
        const response = await authFetch(`/api/citas/horarios-disponibles?fecha=${fecha}${servicioQ}`);
        if (response.ok) {
            const data = await response.json();
            let horarios = data.horariosDisponibles || [];
            
            const hoyFecha = new Date().toISOString().split('T')[0];
            const ahora = new Date();
            const horasActual = ahora.getHours();
            const minutosActual = ahora.getMinutes();
            
            if (fecha === hoyFecha) {
                horarios = horarios.filter(h => {
                    const [hora, minuto] = h.split(':');
                    const horaNum = parseInt(hora);
                    const minutoNum = parseInt(minuto);
                    return (horaNum > horasActual) || (horaNum === horasActual && minutoNum > minutosActual);
                });
            }
            
            const horariosContainer = document.getElementById('horariosContainer');
            if (horarios && horarios.length > 0) {
                horariosContainer.innerHTML = horarios.map(h => 
                    `<button type="button" class="btn btn-outline-primary m-1" style="border-radius: 10px; padding: 10px 20px;" data-hora="${h}" onclick="seleccionarHora(this, '${h}')">${h}</button>`
                ).join('');
                
                if (data.horariosOcupados && data.horariosOcupados.length > 0) {
                    const msgDiv = document.getElementById('disponibilidadMsg');
                    msgDiv.style.display = 'block';
                    msgDiv.className = 'alert alert-warning';
                    msgDiv.innerHTML = `<i class="fas fa-clock"></i> Horarios ocupados: ${data.horariosOcupados.join(', ')}`;
                }
            } else {
                horariosContainer.innerHTML = '<div class="alert alert-danger w-100">No hay horarios disponibles para esta fecha</div>';
                horaActual = null;
            }
        }
    } catch (error) { console.error('Error:', error); }
    setLoading(false);
});

function seleccionarHora(element, hora) {
    document.querySelectorAll('#horariosContainer .btn').forEach(btn => btn.classList.remove('btn-primary', 'selected'));
    document.querySelectorAll('#horariosContainer .btn').forEach(btn => btn.classList.add('btn-outline-primary'));
    element.classList.remove('btn-outline-primary');
    element.classList.add('btn-primary', 'selected');
    horaActual = hora;
    verificarFormularioCompleto();
}

function verificarFormularioCompleto() {
    const btnConfirmar = document.getElementById('btnConfirmar');
    if (servicioActual && fechaActual && horaActual) {
        btnConfirmar.disabled = false;
        btnConfirmar.innerHTML = '<i class="fas fa-check-circle"></i> Confirmar Cita - ' + formatearPrecio(precioActual || 0);
    } else {
        btnConfirmar.disabled = true;
        btnConfirmar.innerHTML = 'Confirmar Cita';
    }
}

function abrirModalCita() {
    servicioActual = null; precioActual = null; fechaActual = null; horaActual = null;
    document.querySelectorAll('.servicio-card').forEach(c => c.classList.remove('selected'));
    document.getElementById('citaFecha').value = '';
    document.getElementById('horariosContainer').innerHTML = '<div class="alert alert-info w-100">Primero selecciona una fecha</div>';
    document.getElementById('disponibilidadMsg').style.display = 'none';
    document.getElementById('precioMostrado').innerHTML = '$0.00';
    document.getElementById('acompananteSection').style.display = 'none';
    document.getElementById('acompananteNombre').value = '';
    document.getElementById('acompananteRelacion').value = '';
    document.getElementById('acompananteTelefono').value = '';
    document.getElementById('acompananteCorreo').value = '';
    document.getElementById('btnConfirmar').disabled = true;
    document.getElementById('btnConfirmar').innerHTML = 'Confirmar Cita';
    cargarServicios();
    new bootstrap.Modal(document.getElementById('citaModal')).show();
}

async function confirmarCita() {
    if (!servicioActual || !fechaActual || !horaActual) {
        mostrarMensaje('Selecciona servicio, fecha y hora', 'danger');
        return;
    }
    // Evita el doble click mientras la petición está en curso
    if (agendando) return;
    agendando = true;
    const btnConfirmar = document.getElementById('btnConfirmar');
    const textoOriginal = btnConfirmar.innerHTML;
    btnConfirmar.disabled = true;
    btnConfirmar.innerHTML = '<span class="spinner-border spinner-border-sm"></span> Agendando...';
    setLoading(true);
    const cita = { fecha: fechaActual, hora: horaActual, servicio: servicioActual, estado: 'AGENDADA' };
    if (servicioActual === 'Terapia de Pareja') {
        cita.acompananteNombre = document.getElementById('acompananteNombre').value;
        cita.acompananteRelacion = document.getElementById('acompananteRelacion').value;
        cita.acompananteTelefono = document.getElementById('acompananteTelefono').value;
        cita.acompananteCorreo = document.getElementById('acompananteCorreo').value;
    }
    try {
        const response = await authFetch('/api/citas', { method: 'POST', body: JSON.stringify(cita) });
        if (response.ok) {
            bootstrap.Modal.getInstance(document.getElementById('citaModal')).hide();
            await cargarCitas();
            mostrarMensaje('✅ Cita agendada exitosamente');
        } else {
            const error = await response.text();
            mostrarMensaje(error, 'danger');
        }
    } catch (error) {
        mostrarMensaje('Error al agendar', 'danger');
    }
    agendando = false;
    btnConfirmar.disabled = false;
    btnConfirmar.innerHTML = textoOriginal;
    setLoading(false);
}

// ==================== PACIENTE - CITAS ====================
async function cargarCitas() {
    setLoading(true);
    try {
        const response = await authFetch('/api/citas');
        if (response.ok) {
            const citas = await response.json();
            const container = document.getElementById('listaCitas');
            const hoyFecha = new Date().toISOString().split('T')[0];
            const ahora = new Date().toTimeString().slice(0, 5);
            
            if (citas.length === 0) {
                container.innerHTML = `<div class="empty-state" style="grid-column:1/-1"><div style="font-size:64px">\u{1F4C5}</div><p class="mt-3 text-muted">Aun no tienes citas agendadas</p><button class="btn btn-primary-custom" onclick="abrirModalCita()">Agendar mi primera cita</button></div>`;
            } else {
                container.innerHTML = citas.map(cita => {
                    const citaPasada = cita.fecha < hoyFecha || (cita.fecha === hoyFecha && cita.hora < ahora);
                    const mostrarCancelar = !citaPasada && cita.estado !== 'CANCELADA' && cita.estado !== 'COMPLETADA';
                    const mostrarReagendar = !citaPasada && cita.estado !== 'CANCELADA' && cita.estado !== 'COMPLETADA';
                    
                    const estadoTexto = { AGENDADA:'Agendada', CANCELADA:'Cancelada', REAGENDADA:'Reagendada', COMPLETADA:'Completada', EN_PROGRESO:'En Progreso' }[cita.estado] || 'Agendada';
                    const partes = partesFecha(cita.fecha);
                    return `<div class="col-12 col-md-6 col-lg-4">
                        <div class="cita-card ${cita.estado === 'REAGENDADA' ? 're-agendada' : ''}">
                            <div class="cita-fecha-bloque">
                                <div class="calendario-mini">
                                    <div class="dia">${partes.dia}</div>
                                    <div class="mes">${partes.mes}</div>
                                </div>
                                <div>
                                    <div class="cita-hora">${cita.hora}</div>
                                    <div class="cita-dia-largo">${partes.largo}</div>
                                </div>
                            </div>
                            <div class="d-flex justify-content-between align-items-start">
                                <div>
                                    <div class="servicio-linea"><i class="fas fa-stethoscope"></i> ${cita.servicio || 'Consulta General'}</div>
                                    ${cita.acompananteNombre ? `<div><i class="fas fa-user-friends"></i> Acompañante: ${cita.acompananteNombre} (${cita.acompananteRelacion || '-'})</div>` : ''}
                                    <div class="precio"><i class="fas fa-dollar-sign"></i> ${formatearPrecio(cita.precio)}</div>
                                    ${cita.estado === 'REAGENDADA' && cita.fechaOriginal ? `<div class="small text-warning mt-1"><i class="fas fa-exchange-alt"></i> Original: ${formatearFecha(cita.fechaOriginal)} ${cita.horaOriginal}</div>` : ''}
                                </div>
                                <span class="pill-estado pill-${(cita.estado || 'AGENDADA').toLowerCase()}">${estadoTexto}</span>
                            </div>
                            <hr>
                            <div class="d-flex gap-2 mt-2">
                                ${mostrarReagendar ? `<button class="btn btn-sm btn-warning" onclick='reagendarCita(${cita.id}, "${cita.fecha}", "${cita.hora}", "${cita.servicio || ''}", ${cita.precio || 0})'><i class="fas fa-calendar-alt"></i> Reagendar</button>` : ''}
                                ${mostrarCancelar ? `<button class="btn btn-sm btn-danger" onclick="cancelarCita(${cita.id})"><i class="fas fa-times"></i> Cancelar</button>` : ''}
                            </div>
                        </div>
                    </div>`;
                }).join('');
                proximaCitaHero(citas, hoyFecha, ahora);
            }
        }
    } catch (error) { mostrarMensaje('Error al cargar tus citas', 'danger'); }
    setLoading(false);
}

/** Muestra en el hero la siguiente cita futura (o la de hoy no completada). */
function proximaCitaHero(citas, hoyFecha, ahora) {
    const proxima = citas
        .filter(c => c.estado !== 'CANCELADA' && c.estado !== 'COMPLETADA' && (c.fecha > hoyFecha || (c.fecha === hoyFecha && c.hora >= ahora)))
        .sort((a, b) => (a.fecha + a.hora).localeCompare(b.fecha + b.hora))[0];
    const hero = document.getElementById('heroProximaCita');
    if (proxima) {
        document.getElementById('heroCitaTexto').innerText =
            formatearFecha(proxima.fecha) + ' \u2022 ' + proxima.hora + ' \u2022 ' + (proxima.servicio || 'Consulta');
        hero.style.display = 'flex';
    } else {
        hero.style.display = 'none';
    }
}

async function cancelarCita(id) {
    if (confirm('¿Estás seguro de cancelar esta cita? Recibirás un correo de confirmación.')) {
        setLoading(true);
        try {
            const response = await authFetch(`/api/citas/${id}/cancelar`, { method: 'PUT' });
            if (response.ok) {
                await cargarCitas();
                mostrarMensaje('✅ Cita cancelada. Se ha enviado un correo de confirmación.', 'success');
            } else {
                const error = await response.text();
                mostrarMensaje(error, 'danger');
            }
        } catch (error) {
            mostrarMensaje('Error al cancelar', 'danger');
        }
        setLoading(false);
    }
}

async function reagendarCita(id, fechaActual, horaActual, servicioActual, precioActual) {
    const hoyFecha = new Date().toISOString().split('T')[0];
    document.getElementById('reagendarId').value = id;
    document.getElementById('reagendarFechaActual').innerText = formatearFecha(fechaActual);
    document.getElementById('reagendarHoraActual').innerText = horaActual;
    document.getElementById('reagendarServicioActual').innerText = servicioActual || 'Consulta General';
    document.getElementById('reagendarPrecioActual').innerHTML = formatearPrecio(precioActual);
    document.getElementById('reagendarMotivo').value = '';
    
    const fechaInputRe = document.getElementById('reagendarFechaNueva');
    fechaInputRe.value = '';
    fechaInputRe.setAttribute('min', hoyFecha);
    document.getElementById('reagendarHorariosContainer').innerHTML = '<div class="alert alert-info w-100">Primero selecciona una fecha</div>';
    document.getElementById('reagendarMensaje').innerHTML = '';
    horaReagendarSeleccionada = null;
    
    fechaInputRe.onchange = async function() {
        const nuevaFecha = this.value;
        if (!nuevaFecha) return;
        setLoading(true);
        try {
            const servicioQ = servicioActual ? `&servicio=${encodeURIComponent(servicioActual)}` : '';
            const response = await authFetch(`/api/citas/horarios-disponibles?fecha=${nuevaFecha}${servicioQ}`);
            if (response.ok) {
                let horarios = (await response.json()).horariosDisponibles || [];
                const ahora = new Date();
                const horasActual = ahora.getHours();
                const minutosActual = ahora.getMinutes();
                if (nuevaFecha === hoyFecha) {
                    horarios = horarios.filter(h => {
                        const [hora, minuto] = h.split(':');
                        const horaNum = parseInt(hora);
                        const minutoNum = parseInt(minuto);
                        return (horaNum > horasActual) || (horaNum === horasActual && minutoNum > minutosActual);
                    });
                }
                const horariosContainer = document.getElementById('reagendarHorariosContainer');
                if (horarios && horarios.length > 0) {
                    horariosContainer.innerHTML = horarios.map(h => 
                        `<button type="button" class="btn btn-outline-primary m-1" style="border-radius: 10px;" onclick="seleccionarHoraReagendar(this, '${h}')">${h}</button>`
                    ).join('');
                } else {
                    horariosContainer.innerHTML = '<div class="alert alert-danger w-100">No hay horarios disponibles para esta fecha</div>';
                }
            }
        } catch (error) { console.error('Error:', error); }
        setLoading(false);
    };
    
    new bootstrap.Modal(document.getElementById('reagendarModal')).show();
}

function seleccionarHoraReagendar(element, hora) {
    document.querySelectorAll('#reagendarHorariosContainer .btn').forEach(btn => {
        btn.classList.remove('btn-primary', 'selected');
        btn.classList.add('btn-outline-primary');
    });
    element.classList.remove('btn-outline-primary');
    element.classList.add('btn-primary', 'selected');
    horaReagendarSeleccionada = hora;
}

async function confirmarReagendar() {
    const id = document.getElementById('reagendarId').value;
    const nuevaFecha = document.getElementById('reagendarFechaNueva').value;
    const motivo = document.getElementById('reagendarMotivo').value;
    
    if (!nuevaFecha) {
        document.getElementById('reagendarMensaje').innerHTML = '<div class="alert alert-danger">Selecciona una fecha</div>';
        return;
    }
    if (!horaReagendarSeleccionada) {
        document.getElementById('reagendarMensaje').innerHTML = '<div class="alert alert-danger">Selecciona una hora</div>';
        return;
    }
    
    // Evita doble click mientras la petición está en curso
    const btnRe = document.querySelector('#reagendarModal .btn-primary:last-of-type') ||
                  document.querySelector('#reagendarModal button.btn-primary');
    if (confirmarReagendar.enviando) return;
    confirmarReagendar.enviando = true;
    if (btnRe) { btnRe.disabled = true; btnRe.dataset.txt = btnRe.innerHTML; btnRe.innerHTML = '<span class="spinner-border spinner-border-sm"></span> Reagendando...'; }
    setLoading(true);
    try {
        const url = `/api/citas/${id}/reagendar?nuevaFecha=${nuevaFecha}&nuevaHora=${horaReagendarSeleccionada}&motivo=${encodeURIComponent(motivo)}`;
        const response = await authFetch(url, { method: 'PUT' });
        if (response.ok) {
            bootstrap.Modal.getInstance(document.getElementById('reagendarModal')).hide();
            await cargarCitas();
            mostrarMensaje('✅ Cita reagendada exitosamente. Se ha enviado un correo de confirmación.', 'success');
        } else {
            const error = await response.text();
            document.getElementById('reagendarMensaje').innerHTML = `<div class="alert alert-danger">${error}</div>`;
        }
    } catch (error) {
        document.getElementById('reagendarMensaje').innerHTML = '<div class="alert alert-danger">Error al reagendar</div>';
    }
    confirmarReagendar.enviando = false;
    if (btnRe) { btnRe.disabled = false; btnRe.innerHTML = btnRe.dataset.txt || 'Confirmar Reagendar'; }
    setLoading(false);
}

// ==================== PACIENTE - HISTORIA CLÍNICA ====================
async function cargarHistoriaClinica() {
    setLoading(true);
    try {
        const res = await authFetch('/api/notas/paciente');
        if (res.ok) {
            const data = await res.json();
            
            const diagHead = n => `<div class="nota-fecha">${n.doctor?.nombreCompleto || n.doctor?.username || 'Doctor'} \u2022 ${new Date(n.fechaCreacion).toLocaleDateString()}</div>`;
            const destacado = document.getElementById('destacadoDiagnostico');
            if (data.diagnosticos?.length > 0) {
                destacado.style.display = 'block';
                document.getElementById('destacadoTexto').innerText = data.diagnosticos[0].diagnostico;
                document.getElementById('destacadoDoctor').innerText =
                    (data.diagnosticos[0].doctor?.nombreCompleto || data.diagnosticos[0].doctor?.username || 'Tu doctor') +
                    ' \u2022 ' + new Date(data.diagnosticos[0].fechaCreacion).toLocaleDateString();
            } else {
                destacado.style.display = 'none';
            }
            
            document.getElementById('diagnosticosContainer').innerHTML = data.diagnosticos?.length > 0 ?
                data.diagnosticos.map(d => `<div class="nota-card">${diagHead(d)}<p class="mt-2 mb-0">${d.diagnostico}</p></div>`).join('') :
                '<p class="text-muted">No hay diagnósticos registrados.</p>';
            
            document.getElementById('recomendacionesContainer').innerHTML = data.recomendaciones?.length > 0 ?
                data.recomendaciones.map(r => `<div class="nota-card">${diagHead(r)}<p class="mt-2 mb-0">${r.recomendaciones}</p></div>`).join('') :
                '<p class="text-muted">No hay recomendaciones registradas.</p>';
            
            document.getElementById('medicamentosContainer').innerHTML = data.medicamentos?.length > 0 ?
                data.medicamentos.map(m => `<div class="nota-card">${diagHead(m)}<p class="mt-2 mb-0">${m.medicamentos}</p></div>`).join('') :
                '<p class="text-muted">No hay medicamentos recetados.</p>';
            
            document.getElementById('consejosContainer').innerHTML = data.consejos?.length > 0 ?
                data.consejos.map(c => `<div class="nota-card">${diagHead(c)}<p class="mt-2 mb-0">${c.recomendaciones || c.diagnostico}</p></div>`).join('') :
                '<p class="text-muted">No hay consejos registrados.</p>';
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

// ==================== PACIENTE - LISTA DE ESPERA ====================
async function agregarListaEspera() {
    const servicioInteres = document.getElementById('servicioInteres').value;
    const fechaDeseada = document.getElementById('fechaDeseada').value;
    const horarioPreferido = document.getElementById('horarioPreferido').value;
    
    if (!servicioInteres) {
        mostrarMensaje('Selecciona un servicio de interés', 'danger');
        return;
    }
    
    setLoading(true);
    try {
        const payload = { servicioInteres };
        if (fechaDeseada) payload.fechaDeseada = fechaDeseada;
        if (horarioPreferido) payload.horarioPreferido = horarioPreferido;
        
        const res = await authFetch('/api/lista-espera', { method: 'POST', body: JSON.stringify(payload) });
        if (res.ok) {
            mostrarMensaje('✅ Te has agregado a la lista de espera', 'success');
            cargarMisSolicitudes();
            document.getElementById('servicioInteres').value = '';
            document.getElementById('fechaDeseada').value = '';
            document.getElementById('horarioPreferido').value = '';
        } else {
            mostrarMensaje(await res.text(), 'danger');
        }
    } catch(e) {
        mostrarMensaje('Error de conexión', 'danger');
    }
    setLoading(false);
}

async function cargarMisSolicitudes() {
    try {
        const res = await authFetch('/api/lista-espera/mis-solicitudes');
        if (res.ok) {
            const solicitudes = await res.json();
            if (solicitudes.length === 0) {
                document.getElementById('misSolicitudes').innerHTML = '<p class="text-muted">No tienes solicitudes activas.</p>';
            } else {
                document.getElementById('misSolicitudes').innerHTML = solicitudes.map(s => `
                    <div class="solicitud-card">
                        <div class="d-flex justify-content-between align-items-center">
                            <div>
                                <strong>${s.servicioInteres}</strong><br>
                                <small class="text-muted">Registrado: ${new Date(s.fechaRegistro).toLocaleString()}</small>
                                ${s.fechaDeseada ? `<br><small class="text-muted">Fecha deseada: ${formatearFecha(s.fechaDeseada)}</small>` : ''}
                                ${s.horarioPreferido ? `<br><small class="text-muted">Horario preferido: ${s.horarioPreferido}</small>` : ''}
                            </div>
                            <div class="text-end">
                                <span class="pill-estado ${s.notificado ? 'pill-notificado' : 'pill-espera'}">${s.notificado ? 'Notificado' : 'En espera'}</span>
                                <button class="btn btn-sm btn-danger d-block mt-2" onclick="eliminarSolicitud(${s.id})">Cancelar</button>
                            </div>
                        </div>
                    </div>
                `).join('');
            }
        }
    } catch(e) { console.error(e); }
}

async function eliminarSolicitud(id) {
    if (confirm('¿Cancelar esta solicitud de lista de espera?')) {
        setLoading(true);
        try {
            const res = await authFetch(`/api/lista-espera/${id}`, { method: 'DELETE' });
            if (res.ok) {
                mostrarMensaje('✅ Solicitud eliminada', 'success');
                cargarMisSolicitudes();
            } else {
                mostrarMensaje('Error al eliminar', 'danger');
            }
        } catch(e) {
            mostrarMensaje('Error de conexión', 'danger');
        }
        setLoading(false);
    }
}

// ==================== PACIENTE - MENSAJES ====================
async function cargarMensajes() {
    setLoading(true);
    try {
        const res = await authFetch('/api/mensajes/recibidos');
        if (res.ok) {
            const mensajes = await res.json();
            document.getElementById('mensajesContainer').innerHTML = mensajes.map(m => `
                <div class="mensaje-card" onclick="marcarComoLeido(${m.id})" style="cursor:pointer;">
                    <div class="d-flex justify-content-between align-items-start">
                        <div class="mensaje-asunto">${m.asunto}${!m.leido ? ' <span class="pill-estado pill-en_progreso">Nuevo</span>' : ''}</div>
                        <div class="mensaje-meta">${new Date(m.fechaEnvio).toLocaleString()}</div>
                    </div>
                    <div class="mensaje-meta">De: ${m.remitente?.username}</div>
                    <p class="mt-2 mb-0">${m.contenido}</p>
                </div>
            `).join('');
            if (mensajes.length === 0) document.getElementById('mensajesContainer').innerHTML = '<p class="text-muted">No tienes mensajes.</p>';
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

async function marcarComoLeido(id) {
    try {
        await authFetch(`/api/mensajes/${id}/leer`, { method: 'PUT' });
        cargarMensajes();
        cargarMensajesNoLeidos();
    } catch(e) { console.error(e); }
}

async function enviarMensaje() {
    const destinatario = document.getElementById('nuevoMensajeDestinatario').value;
    const asunto = document.getElementById('nuevoMensajeAsunto').value;
    const contenido = document.getElementById('nuevoMensajeContenido').value;
    
    if (!destinatario || !asunto || !contenido) {
        mostrarMensaje('Completa todos los campos', 'danger');
        return;
    }
    
    setLoading(true);
    try {
        const res = await authFetch('/api/mensajes', { method: 'POST', body: JSON.stringify({ destinatario, asunto, contenido }) });
        if (res.ok) {
            document.getElementById('nuevoMensajeDestinatario').value = '';
            document.getElementById('nuevoMensajeAsunto').value = '';
            document.getElementById('nuevoMensajeContenido').value = '';
            cargarMensajes();
            mostrarMensaje('✅ Mensaje enviado');
        } else {
            mostrarMensaje('Error al enviar', 'danger');
        }
    } catch(e) {
        mostrarMensaje('Error', 'danger');
    }
    setLoading(false);
}

function abrirModalMensajes() {
    document.querySelector('[data-tab="mensajes"]').click();
}