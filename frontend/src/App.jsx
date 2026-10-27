import { useEffect, useState } from 'react';
import {
  Routes, Route, NavLink, Link, Navigate, useLocation, useNavigate,
} from 'react-router-dom';
import { CalendarDays, LogOut } from 'lucide-react';
import { Ctx, api } from './api.js';
import Home from './Home.jsx';
import EventPage from './EventPage.jsx';
import Mine from './Mine.jsx';
import { Login, Signup } from './AuthPages.jsx';

function readSavedUser() {
  try {
    const token = localStorage.getItem('cc_token');
    return token ? JSON.parse(localStorage.getItem('cc_user') || 'null') : null;
  } catch {
    return null;
  }
}

export default function App() {
  const [user, setUser] = useState(readSavedUser);
  const [toastMessage, setToastMessage] = useState(null);
  const location = useLocation();
  const navigate = useNavigate();

  function toast(message, ok = true) {
    setToastMessage({ message, ok });
    window.setTimeout(() => setToastMessage(null), 3500);
  }

  function login(data) {
    const { token, ...profile } = data;
    localStorage.setItem('cc_token', token);
    localStorage.setItem('cc_user', JSON.stringify(profile));
    setUser(profile);
  }

  function logout() {
    localStorage.removeItem('cc_token');
    localStorage.removeItem('cc_user');
    setUser(null);
    toast('You have been signed out.');
    navigate('/login', { replace: true });
  }

  useEffect(() => {
    if (!localStorage.getItem('cc_token')) return;
    api.get('/auth/me')
      .then(({ data }) => {
        const profile = { id: data.id, name: data.name, email: data.email, role: data.role };
        localStorage.setItem('cc_user', JSON.stringify(profile));
        setUser(profile);
      })
      .catch(() => {
        localStorage.removeItem('cc_token');
        localStorage.removeItem('cc_user');
        setUser(null);
      });
  }, []);

  const protectedPage = (element) => user
    ? element
    : <Navigate to="/login" replace state={{ from: location.pathname }} />;

  return (
    <Ctx.Provider value={{ user, uid: user?.id, toast, login }}>
      {user && (
        <header className="nav">
          <Link to="/" className="logo"><CalendarDays size={22} /> CampusConnect</Link>
          <nav>
            <NavLink to="/" end>Discover Events</NavLink>
            <NavLink to="/registrations">My Registrations</NavLink>
          </nav>
          <div className="actions">
            <span className="muted">Hi, {user.name}</span>
            <button className="btn alt" onClick={logout}><LogOut size={14} /> Sign out</button>
          </div>
        </header>
      )}

      <Routes>
        <Route path="/login" element={user ? <Navigate to="/" replace /> : <Login />} />
        <Route path="/signup" element={user ? <Navigate to="/" replace /> : <Signup />} />
        <Route path="/" element={protectedPage(<Home />)} />
        <Route path="/events/:id" element={protectedPage(<EventPage key={user?.id} />)} />
        <Route path="/registrations" element={protectedPage(<Mine key={user?.id} />)} />
        <Route path="*" element={<Navigate to={user ? '/' : '/login'} replace />} />
      </Routes>

      {user && <footer className="foot muted">CampusConnect · CSC2104 OOP Mini-Project</footer>}
      {toastMessage && <div role="status" className={`toast ${toastMessage.ok ? 'ok' : 'err'}`}>{toastMessage.message}</div>}
    </Ctx.Provider>
  );
}
