import { useContext, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Bell, Calendar, CheckCircle, Clock, XCircle } from 'lucide-react';
import { api, errMsg, fmt, Ctx } from './api.js';
import { Badge } from './EventCard.jsx';

export default function Mine() {
  const { uid, toast } = useContext(Ctx);
  const [regs, setRegs] = useState(null);
  const [notes, setNotes] = useState([]);
  const [err, setErr] = useState(null);
  const [cancelling, setCancelling] = useState(null); // eventId being cancelled

  const load = () => {
    api.get(`/users/${uid}/registrations`)
      .then(r => setRegs(r.data))
      .catch(e => setErr(errMsg(e)));
    api.get(`/users/${uid}/notifications`)
      .then(r => setNotes(r.data))
      .catch(() => {});
  };

  useEffect(load, []);

  const cancel = async r => {
    setCancelling(r.eventId);
    try {
      await api.delete(`/events/${r.eventId}/registrations/me`, { params: { userId: uid } });
      toast('Registration cancelled.');
    } catch (e) {
      toast(errMsg(e), false);
    }
    setCancelling(null);
    load();
  };

  const n = s => regs?.filter(r => r.status === s).length ?? 0;

  const activeRegs = regs?.filter(r => r.status !== 'CANCELLED') ?? [];
  const cancelledRegs = regs?.filter(r => r.status === 'CANCELLED') ?? [];

  return (
    <>
      {/* Page hero */}
      <section className="page-hero" aria-labelledby="mine-heading">
        <h1 id="mine-heading">My Registrations</h1>
        <p>Track your upcoming events and manage your schedule</p>
      </section>

      <div className="wrap" style={{ paddingTop: 28 }}>
        {/* Error */}
        {err && (
          <div className="alert-box" role="alert" aria-live="assertive">
            <div className="alert-box-content">
              <span className="alert-icon" aria-hidden="true">⚠️</span>
              <span>{err}</span>
            </div>
            <button className="btn btn-ghost btn-sm" onClick={load}>Retry</button>
          </div>
        )}

        {/* Stats */}
        {regs && (
          <div className="stats-grid" aria-label="Registration summary">
            <div className="stat-card confirmed">
              <CheckCircle size={20} style={{ color: '#16a34a', marginBottom: 8 }} aria-hidden="true" />
              <span className="stat-number">{n('CONFIRMED')}</span>
              <span className="stat-label">Confirmed</span>
            </div>
            <div className="stat-card waitlisted">
              <Clock size={20} style={{ color: '#d97706', marginBottom: 8 }} aria-hidden="true" />
              <span className="stat-number">{n('WAITLISTED')}</span>
              <span className="stat-label">On Waitlist</span>
            </div>
            <div className="stat-card cancelled">
              <XCircle size={20} style={{ color: '#dc2626', marginBottom: 8 }} aria-hidden="true" />
              <span className="stat-number">{n('CANCELLED')}</span>
              <span className="stat-label">Cancelled</span>
            </div>
          </div>
        )}

        {/* Empty state */}
        {regs && regs.length === 0 && (
          <div className="empty-state" role="status">
            <div className="empty-state-icon" aria-hidden="true">🎟️</div>
            <h3>No registrations yet</h3>
            <p>Find something exciting and sign up — it only takes one click.</p>
            <Link to="/" className="btn btn-amber">Discover Events</Link>
          </div>
        )}

        {/* Active registrations table */}
        {regs && activeRegs.length > 0 && (
          <section aria-labelledby="active-heading">
            <h2 id="active-heading" style={{ marginBottom: 16 }}>
              <Calendar size={20} style={{ verticalAlign: 'middle', marginRight: 8, color: 'var(--sage)' }} aria-hidden="true" />
              Upcoming &amp; Active
            </h2>
            <div className="reg-table" role="table" aria-label="Active registrations">
              <div className="reg-table-header" role="row" aria-hidden="true">
                <span>Event</span>
                <span>Registered on</span>
                <span>Status</span>
                <span>Action</span>
              </div>
              {activeRegs.map(r => (
                <div key={r.eventId} className="reg-row" role="row">
                  <Link to={`/events/${r.eventId}`} className="reg-event-link" role="cell">
                    {r.title}
                  </Link>
                  <span className="reg-date" role="cell">
                    <span aria-hidden="true">
                      <Calendar size={12} style={{ verticalAlign: 'middle', marginRight: 4 }} />
                    </span>
                    {fmt(r.registeredAt)}
                  </span>
                  <span role="cell"><Badge s={r.status} /></span>
                  <span role="cell">
                    <button
                      id={`cancel-btn-${r.eventId}`}
                      className={`btn btn-danger btn-sm ${cancelling === r.eventId ? 'btn-spinning' : ''}`}
                      disabled={cancelling === r.eventId}
                      onClick={() => cancel(r)}
                      aria-busy={cancelling === r.eventId}
                      aria-label={`Cancel registration for ${r.title}`}
                    >
                      {cancelling === r.eventId ? 'Cancelling…' : 'Cancel'}
                    </button>
                  </span>
                </div>
              ))}
            </div>
          </section>
        )}

        {/* Cancelled registrations */}
        {regs && cancelledRegs.length > 0 && (
          <section aria-labelledby="cancelled-heading" style={{ marginTop: 32 }}>
            <h2 id="cancelled-heading" style={{ marginBottom: 16 }}>
              <XCircle size={20} style={{ verticalAlign: 'middle', marginRight: 8, color: '#dc2626' }} aria-hidden="true" />
              Cancelled
            </h2>
            <div className="reg-table" role="table" aria-label="Cancelled registrations" style={{ opacity: .75 }}>
              <div className="reg-table-header" role="row" aria-hidden="true">
                <span>Event</span>
                <span>Registered on</span>
                <span>Status</span>
                <span></span>
              </div>
              {cancelledRegs.map(r => (
                <div key={r.eventId} className="reg-row" role="row">
                  <Link to={`/events/${r.eventId}`} className="reg-event-link" role="cell">{r.title}</Link>
                  <span className="reg-date" role="cell">{fmt(r.registeredAt)}</span>
                  <span role="cell"><Badge s={r.status} /></span>
                  <span role="cell" />
                </div>
              ))}
            </div>
          </section>
        )}

        {/* Notifications */}
        <section className="notif-section" aria-labelledby="notif-heading" style={{ marginTop: 40 }}>
          <h2 id="notif-heading" style={{ marginBottom: 16 }}>
            <Bell size={20} style={{ verticalAlign: 'middle', marginRight: 8, color: 'var(--amber)' }} aria-hidden="true" />
            Notifications
          </h2>
          {notes.length === 0 ? (
            <p className="muted">No notifications yet. Register for events to get updates.</p>
          ) : (
            <ul className="notif-list" aria-label="Your notifications">
              {notes.map(x => (
                <li key={x.id} className="notif-item">
                  <span className="notif-dot" aria-hidden="true" />
                  <span className="notif-text">{x.message}</span>
                  <span className="notif-time" aria-label={`Received ${fmt(x.createdAt)}`}>{fmt(x.createdAt)}</span>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </>
  );
}
