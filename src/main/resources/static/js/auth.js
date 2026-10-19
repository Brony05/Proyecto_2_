// ==================== AUTENTICACIÓN ====================

async function logout() {
    setLoading(true);
    try {
        const tokenActual = getToken();
        if (tokenActual) {
            await fetch('/auth/logout', {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${tokenActual}` }
            }).catch(e => console.log('Error:', e));
        }
    } catch (error) {
        console.error('Error:', error);
    } finally {
        localStorage.clear();
        window.location.href = '/';
    }
    setLoading(false);
}

function verificarSesion() {
    const token = localStorage.getItem('token');
    const rol = localStorage.getItem('rol');
    
    if (!token) {
        window.location.href = '/';
        return null;
    }
    
    return { token, rol };
}

function redirigirPorRol(rol) {
    if (rol === 'ADMIN') window.location.href = '/admin.html';
    else if (rol === 'DIRECTOR') window.location.href = '/director.html';
    else if (rol === 'EMPLEADO') window.location.href = '/empleado.html';
    else if (rol === 'PACIENTE') window.location.href = '/paciente.html';
    else if (rol === 'DOCTOR') window.location.href = '/doctor.html';
    else if (rol === 'PSICOLOGO') window.location.href = '/psicologo.html';
    else if (rol === 'PSIQUIATRA') window.location.href = '/psiquiatra.html';
    else if (rol === 'ENFERMERO') window.location.href = '/enfermero.html';
    else if (rol === 'TRABAJADOR_SOCIAL') window.location.href = '/trabajador-social.html';
    else if (rol === 'ADMINISTRATIVO') window.location.href = '/administrativo.html';
    else if (rol === 'JEFE_PSICOLOGIA' || rol === 'JEFE_PSIQUIATRIA' || 
             rol === 'JEFE_TRABAJO_SOCIAL' || rol === 'JEFE_ENFERMERIA') window.location.href = '/jefe.html';
    else if (rol === 'JEFE_ARCHIVO') window.location.href = '/jefe-archivo.html';
    else if (rol === 'COMITE_ETICA') window.location.href = '/comite-etica.html';
    else window.location.href = '/';
}