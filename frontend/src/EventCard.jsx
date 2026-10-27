import { Link } from 'react-router-dom';
import { Calendar, MapPin, Users, Star } from 'lucide-react';
import { fmt } from './api.js';

export const Badge = ({ s }) => (
  <span className={`badge ${String(s).toLowerCase()}`} aria-label={`Status: ${s}`}>
    {s.charAt(0) + s.slice(1).toLowerCase()}
  </span>
);

export default function EventCard({ e }) {
  const pct = Math.min(100, Math.round((e.registered / e.capacity) * 100));
  const isFull = e.seatsLeft === 0;
  const isFree = e.price === 0;

  return (
    <article className="card" aria-label={`Event: ${e.title}`}>
      {/* Image */}
      <div
        className="card-img"
        style={{ backgroundImage: e.imageUrl ? `url(${e.imageUrl})` : undefined }}
        role="img"
        aria-label={`Cover image for ${e.title}`}
      >
        <div className="card-img-overlay" aria-hidden="true" />
        <div className="card-chips" aria-hidden="true">
          <span className="chip">{e.category}</span>
          {e.featured && (
            <span className="chip-featured">
              <Star size={10} fill="currentColor" /> Featured
            </span>
          )}
        </div>
      </div>

      {/* Body */}
      <div className="card-body">
        <h3 className="card-title">{e.title}</h3>

        <div className="card-meta">
          <p className="card-row">
            <Calendar size={14} aria-hidden="true" />
            <span>{fmt(e.startTime)}</span>
          </p>
          <p className="card-row">
            <MapPin size={14} aria-hidden="true" />
            <span>{e.venue}</span>
          </p>
          <p className="card-row">
            <Users size={14} aria-hidden="true" />
            <span className={isFull ? 'cap-text full-label' : 'cap-text'}>
              {isFull ? 'Full · waitlist open' : `${e.seatsLeft} seat${e.seatsLeft !== 1 ? 's' : ''} left`}
            </span>
          </p>
        </div>

        {/* Capacity bar */}
        <div
          className="cap-bar"
          role="progressbar"
          aria-valuenow={pct}
          aria-valuemin={0}
          aria-valuemax={100}
          aria-label={`${pct}% capacity filled`}
        >
          <div className={`cap-bar-fill ${isFull ? 'full' : ''}`} style={{ width: pct + '%' }} />
        </div>

        <div className="card-footer">
          <span className="card-organizer">{e.organizer}</span>
          <span className={`card-price ${isFree ? 'free' : ''}`}>
            {isFree ? 'Free' : `$${e.price}`}
          </span>
        </div>

        <Link className="btn-card" to={`/events/${e.id}`} aria-label={`View details for ${e.title}`}>
          View Details
        </Link>
      </div>
    </article>
  );
}
