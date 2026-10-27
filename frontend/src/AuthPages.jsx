import { useContext, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { ArrowRight, CalendarDays } from 'lucide-react';
import { api, Ctx, errMsg } from './api.js';

function AuthFrame({ title, subtitle, children, footer }) {
  return (
    <main className="auth-screen">
      <section className="auth-card">
        <Link to="/login" className="auth-brand"><CalendarDays size={22} /> CampusConnect</Link>
        <p className="auth-eyebrow">YOUR CAMPUS, CONNECTED</p>
        <h1>{title}</h1>
        <p className="auth-subtitle">{subtitle}</p>
        {children}
        <div className="auth-switch">{footer}</div>
      </section>
      <aside className="auth-art">
        <span className="auth-art-mark">✳</span>
        <p>Find your people.<br /><i>Find your next thing.</i></p>
        <small>GOOD THINGS HAPPEN ON CAMPUS</small>
      </aside>
    </main>
  );
}

export function Login() {
  const { login } = useContext(Ctx);
  const location = useLocation();
  const navigate = useNavigate();
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(event) {
    event.preventDefault();
    setError('');
    setBusy(true);
    const form = new FormData(event.currentTarget);
    try {
      const { data } = await api.post('/auth/signin', {
        email: form.get('email'),
        password: form.get('password'),
      });
      login(data);
      navigate(location.state?.from || '/', { replace: true });
    } catch (err) {
      setError(errMsg(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthFrame title="Welcome back." subtitle="Sign in to see what’s happening on campus."
      footer={<>New to CampusConnect? <Link to="/signup">Create an account</Link></>}>
      <form className="auth-form" onSubmit={submit}>
        <label>Campus email<input name="email" type="email" autoComplete="email" required placeholder="you@campus.edu" /></label>
        <label>Password<input name="password" type="password" autoComplete="current-password" required placeholder="Your password" /></label>
        {error && <p className="auth-error" role="alert">{error}</p>}
        <button className="auth-submit" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'} <ArrowRight size={16} /></button>
      </form>
    </AuthFrame>
  );
}

export function Signup() {
  const { login } = useContext(Ctx);
  const navigate = useNavigate();
  const [role, setRole] = useState('STUDENT');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(event) {
    event.preventDefault();
    setError('');
    setBusy(true);
    const form = new FormData(event.currentTarget);
    const payload = {
      name: form.get('name'),
      email: form.get('email'),
      password: form.get('password'),
      role,
      ...(role === 'STUDENT' ? { studentNumber: form.get('studentNumber') } : { club: form.get('club') }),
    };
    try {
      const { data } = await api.post('/auth/signup', payload);
      login(data);
      navigate('/', { replace: true });
    } catch (err) {
      setError(errMsg(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthFrame title="Join your campus." subtitle="Create an account to register and keep track of events."
      footer={<>Already have an account? <Link to="/login">Sign in</Link></>}>
      <form className="auth-form" onSubmit={submit}>
        <label>Full name<input name="name" required minLength="2" autoComplete="name" /></label>
        <label>Campus email<input name="email" type="email" required autoComplete="email" placeholder="you@campus.edu" /></label>
        <label>Password<input name="password" type="password" required minLength="8" autoComplete="new-password" placeholder="At least 8 characters" /></label>
        <label>Account type<select value={role} onChange={(event) => setRole(event.target.value)}><option value="STUDENT">Student</option><option value="ORGANIZER">Organizer</option></select></label>
        {role === 'STUDENT'
          ? <label>Student ID<input name="studentNumber" required /></label>
          : <label>Club or organization<input name="club" required /></label>}
        {error && <p className="auth-error" role="alert">{error}</p>}
        <button className="auth-submit" disabled={busy}>{busy ? 'Creating account…' : 'Create account'} <ArrowRight size={16} /></button>
      </form>
    </AuthFrame>
  );
}
