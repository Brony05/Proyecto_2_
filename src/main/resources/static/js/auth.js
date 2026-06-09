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
    else if (rol === 'EMPLEADO') window.location.href = '/empleado.html';
    else if (rol === 'PACIENTE') window.location.href = '/paciente.html';
    else if (rol === 'DOCTOR') window.location.href = '/doctor.html';
    else window.location.href = '/';
}