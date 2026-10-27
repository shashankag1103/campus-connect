import { useContext, useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  Calendar, MapPin, Clock, Mail, Users, ChevronLeft,
  Ticket, Info, CheckCircle, XCircle, AlertCircle, Star
} from 'lucide-react';
import { api, errMsg, fmt, Ctx } from './api.js';
import EventCard, { Badge } from './EventCard.jsx';

export default function EventPage() {
  const { id } = useParams();
  const { uid, toast } = useContext(Ctx);
  const [e, setE] = useState(null);
  const [err, setErr] = useState(null);
  const [mine, setMine] = useState(null); // registration status or null
  const [rel, setRel] = useState([]);
  const [busy, setBusy] = useState(false);

  const load = () => {
    setErr(null);
    api.get(`/events/${id}`)
      .then(r => {
        setE(r.data);
        return api.get('/events', { params: { category: r.data.category, size: 4 } });
      })
      .then(r => setRel(r.data.items.filter(x => x.id !== +id)))
      .catch(x => setErr(errMsg(x)));

    api.get(`/users/${uid}/registrations`)
      .then(r => {
        const active = r.data.find(m => m.eventId === +id && m.status !== 'CANCELLED');
        setMine(active?.status || null);
      })
      .catch(() => {});
  };

  useEffect(load, [id]);

  const act = async (fn, successMsg) => {
    setBusy(true);
    try {
      await fn();
      toast(successMsg);
    } catch (x) {
      toast(errMsg(x), false);
    }
    setBusy(false);
    load();
  };

  const handleRegister = () =>
    act(
      () => api.post(`/events/${id}/registrations`, null, { params: { userId: uid, waitlist: e.seatsLeft === 0 } }),
      e.seatsLeft === 0 ? 'Added to waitlist!' : '🎉 You are registered!'
    );

  const handleCancel = () =>
    act(
      () => api.delete(`/events/${id}/registrations/me`, { params: { userId: uid } }),
      'Registration cancelled.'
    );

  /* ── Error state ── */
  if (err) return (
    <div className="wrap" style={{ paddingTop: 32 }}>
      <Link to="/" className="back-link"><ChevronLeft size={16} /> Back to events</Link>
      <div className="alert-box" role="alert">
        <div className="alert-box-content">
          <span className="alert-icon" aria-hidden="true">⚠️</span>
          <span>{err}</span>
        </div>
      </div>
    </div>
  );

  /* ── Loading state ── */
  if (!e) return (
    <div className="wrap" style={{ paddingTop: 32 }}>
      <div className="skel" style={{ height: 320, borderRadius: 20, marginBottom: 24 }} aria-label="Loading event…" />
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 360px', gap: 32 }}>
        <div>
          <div className="skel" style={{ height: 32, width: '60%', marginBottom: 12 }} />
          <div className="skel" style={{ height: 120 }} />
        </div>
        <div className="skel" style={{ height: 380 }} />
      </div>
    </div>
  );

  const closed = e.status !== 'PUBLISHED' || new Date() > new Date(e.registrationDeadline);
  const pct = Math.min(100, Math.round((e.registered / e.capacity) * 100));
  const isFull = e.seatsLeft === 0;
  const isFree = e.price === 0;
  const agendaItems = (e.agenda || '').split('|').filter(Boolean);
  const tagItems = (e.tags || '').split(',').filter(Boolean);

  /* ── Registration button logic ── */
  let regButton = null;
  if (mine) {
    regButton = (
      <button
        id="cancel-reg-btn"
        className={`btn btn-danger btn-full ${busy ? 'btn-spinning' : ''}`}
        disabled={busy}
        onClick={handleCancel}
        aria-busy={busy}
      >
        <XCircle size={16} aria-hidden="true" />
        {busy ? 'Cancelling…' : 'Cancel Registration'}
      </button>
    );
  } else if (closed) {
    regButton = (
      <button id="reg-closed-btn" className="btn btn-full" disabled aria-disabled="true">
        <AlertCircle size={16} aria-hidden="true" />
        Registration Closed
      </button>
    );
  } else {
    regButton = (
      <button
        id="register-btn"
        className={`btn btn-amber btn-full ${busy ? 'btn-spinning' : ''}`}
        disabled={busy}
        onClick={handleRegister}
        aria-busy={busy}
      >
        <Ticket size={16} aria-hidden="true" />
        {busy ? 'Processing…' : isFull ? 'Join Waitlist' : 'Register Now'}
      </button>
    );
  }

  return (
    <div className="wrap" style={{ paddingTop: 20 }}>
      {/* Back link */}
      <Link to="/" className="back-link">
        <ChevronLeft size={16} aria-hidden="true" /> Back to events
      </Link>

      {/* Banner image */}
      <div
        className="detail-banner"
        style={{ backgroundImage: e.imageUrl ? `url(${e.imageUrl})` : undefined }}
        role="img"
        aria-label={`Cover image for ${e.title}`}
      >
        <div className="detail-banner-overlay" aria-hidden="true" />
        <div className="detail-banner-bottom" aria-hidden="true">
          <div className="detail-banner-category">
            <span className="chip">{e.category}</span>
            {e.featured && <span className="chip-featured" style={{ marginLeft: 8 }}><Star size={10} fill="currentColor" /> Featured</span>}
          </div>
        </div>
      </div>

      {/* Two-column layout */}
      <div className="detail-layout">
        {/* ── Main column ── */}
        <div className="detail-main">
          <h1>{e.title}</h1>
          <p className="organizer-line">
            <Users size={15} aria-hidden="true" style={{ color: 'var(--sage)' }} />
            Organised by <strong>{e.organizer}</strong>
            {e.contact && <> · <a href={`mailto:${e.contact}`}>{e.contact}</a></>}
          </p>

          {/* About */}
          <div className="detail-section">
            <h2>About this Event</h2>
            <p>{e.description}</p>
          </div>

          {/* Agenda */}
          {agendaItems.length > 0 && (
            <div className="detail-section">
              <h2>Agenda</h2>
              <ul className="agenda-list" aria-label="Event agenda">
                {agendaItems.map((item, i) => (
                  <li key={i}>
                    <span className="agenda-num" aria-hidden="true">{i + 1}</span>
                    <span>{item.trim()}</span>
                  </li>
                ))}
              </ul>
            </div>
          )}

          {/* Eligibility */}
          {e.eligibility && (
            <div className="detail-section">
              <h2>Eligibility &amp; Accessibility</h2>
              <p>{e.eligibility}</p>
            </div>
          )}

          {/* Tags */}
          {tagItems.length > 0 && (
            <div className="tags-row" aria-label="Event tags">
              {tagItems.map(t => (
                <span key={t} className="tag">#{t.trim()}</span>
              ))}
            </div>
          )}
        </div>

        {/* ── Sidebar ── */}
        <aside className="detail-sidebar" aria-label="Registration and event info">
          <div className="sidebar-card">
            {/* Price header */}
            <div className="sidebar-card-head">
              <p className="price-label">Registration fee</p>
              <p className={`price-value ${isFree ? 'free' : ''}`}>
                {isFree ? 'Free' : `$${e.price.toFixed(2)}`}
              </p>
            </div>

            {/* Info rows */}
            <div className="sidebar-card-body">
              <div className="info-row">
                <Calendar size={16} aria-hidden="true" />
                <div className="info-row-text">
                  <p className="info-row-label">Start</p>
                  <strong>{fmt(e.startTime)}</strong>
                </div>
              </div>
              <div className="info-row">
                <Clock size={16} aria-hidden="true" />
                <div className="info-row-text">
                  <p className="info-row-label">End</p>
                  <strong>{fmt(e.endTime)}</strong>
                </div>
              </div>
              <div className="info-row">
                <MapPin size={16} aria-hidden="true" />
                <div className="info-row-text">
                  <p className="info-row-label">Venue</p>
                  <strong>{e.venue}</strong>
                </div>
              </div>
              {e.contact && (
                <div className="info-row">
                  <Mail size={16} aria-hidden="true" />
                  <div className="info-row-text">
                    <p className="info-row-label">Contact</p>
                    <a href={`mailto:${e.contact}`}>{e.contact}</a>
                  </div>
                </div>
              )}

              {/* Capacity */}
              <div className="cap-section">
                <div className="cap-numbers">
                  <span><strong style={{ color: 'var(--forest)' }}>{e.registered}</strong> / {e.capacity} registered</span>
                  <span style={{ color: isFull ? '#dc2626' : 'var(--forest-light)', fontWeight: 700 }}>
                    {isFull ? 'Full' : `${e.seatsLeft} left`}
                  </span>
                </div>
                <div
                  className="cap-bar"
                  role="progressbar"
                  aria-valuenow={pct}
                  aria-valuemin={0}
                  aria-valuemax={100}
                  aria-label={`${pct}% of capacity filled`}
                >
                  <div className={`cap-bar-fill ${isFull ? 'full' : ''}`} style={{ width: pct + '%' }} />
                </div>
              </div>

              {/* Current registration status */}
              {mine && (
                <div className={`reg-status-box ${mine === 'WAITLISTED' ? 'full' : ''}`}>
                  <CheckCircle size={15} aria-hidden="true" />
                  <span>Your status: <Badge s={mine} /></span>
                </div>
              )}

              {/* Registration closed notice */}
              {closed && !mine && (
                <div className="reg-status-box closed" aria-live="polite">
                  <Info size={15} aria-hidden="true" />
                  <span>
                    {e.status !== 'PUBLISHED' ? `Event is ${e.status.toLowerCase()}` : 'Registration deadline passed'}
                  </span>
                </div>
              )}

              {/* Full / waitlist notice */}
              {isFull && !closed && !mine && (
                <div className="reg-status-box full">
                  <AlertCircle size={15} aria-hidden="true" />
                  <span>Event is full — you'll be added to the waitlist</span>
                </div>
              )}

              {/* Action button */}
              <div className="sidebar-action">{regButton}</div>

              {/* Deadline note */}
              {!closed && (
                <p className="deadline-note" aria-label={`Register by ${fmt(e.registrationDeadline)}`}>
                  Register by {fmt(e.registrationDeadline)}
                </p>
              )}
            </div>
          </div>
        </aside>
      </div>

      {/* Related events */}
      {rel.length > 0 && (
        <section className="related-section" aria-labelledby="related-heading">
          <h2 id="related-heading">More {e.category} Events</h2>
          <div className="grid" role="list" aria-label="Related events">
            {rel.map(r => <EventCard key={r.id} e={r} />)}
          </div>
        </section>
      )}
    </div>
  );
}
