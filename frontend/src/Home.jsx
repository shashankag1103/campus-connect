import { useEffect, useState } from 'react';
import { Search, SlidersHorizontal, Star, ChevronLeft, ChevronRight, Zap } from 'lucide-react';
import { api, errMsg, CATEGORIES } from './api.js';
import EventCard from './EventCard.jsx';

export default function Home() {
  const [q, setQ] = useState('');
  const [cat, setCat] = useState('');
  const [sort, setSort] = useState('soon');
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [feat, setFeat] = useState([]);
  const [err, setErr] = useState(null);
  const [loading, setLoading] = useState(false);

  const load = (qq = q) => {
    setData(null);
    setErr(null);
    setLoading(true);
    api.get('/events', { params: { q: qq || undefined, category: cat || undefined, sort, page, size: 9 } })
      .then(r => setData(r.data))
      .catch(e => setErr(errMsg(e)))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, [cat, sort, page]);

  useEffect(() => {
    api.get('/events', { params: { featured: true, size: 3 } })
      .then(r => setFeat(r.data.items))
      .catch(() => {});
  }, []);

  const handleSearch = e => {
    e.preventDefault();
    setPage(0);
    load();
  };

  const handleCat = c => {
    setPage(0);
    setCat(c);
  };

  const noActiveFilters = !cat && !q;

  return (
    <>
      {/* ── Hero ── */}
      <section className="hero" aria-labelledby="hero-heading">
        <div className="hero-inner">
          <p className="hero-eyebrow" aria-hidden="true">
            <Zap size={12} /> University Events Platform
          </p>
          <h1 id="hero-heading">Discover What's Happening on Campus</h1>
          <p className="hero-sub">
            Hackathons, workshops, sports and cultural nights. Find them, register in one click, and never miss out.
          </p>

          {/* Search */}
          <form className="search-form" onSubmit={handleSearch} role="search" aria-label="Search events">
            <span className="search-icon" aria-hidden="true">
              <Search size={18} />
            </span>
            <input
              id="event-search"
              type="search"
              aria-label="Search by event, organiser or venue"
              placeholder="Search by event, organiser or venue…"
              value={q}
              onChange={e => setQ(e.target.value)}
            />
            <button type="submit" className="btn-search" aria-label="Run search">
              Search
            </button>
          </form>

          {/* Category chips */}
          <div className="hero-chips" role="group" aria-label="Filter by category">
            {['', ...CATEGORIES].map(c => (
              <button
                key={c}
                className={`pill ${cat === c ? 'on' : ''}`}
                onClick={() => handleCat(c)}
                aria-pressed={cat === c}
                aria-label={c ? `Filter: ${c}` : 'Show all categories'}
              >
                {c || 'All'}
              </button>
            ))}
          </div>
        </div>
      </section>

      {/* ── Featured ── */}
      {feat.length > 0 && noActiveFilters && (
        <section className="wrap" aria-labelledby="featured-heading">
          <div className="section-head">
            <div>
              <p className="featured-label" aria-hidden="true">
                <Star size={14} fill="currentColor" /> Featured &amp; Trending
              </p>
              <h2 id="featured-heading" style={{ marginTop: 0 }}>Don't miss these</h2>
            </div>
          </div>
          <div className="grid" role="list" aria-label="Featured events">
            {feat.map(e => <EventCard key={e.id} e={e} />)}
          </div>
        </section>
      )}

      {/* ── All Events ── */}
      <section className="wrap" aria-labelledby="events-heading">
        {/* Filter row */}
        <div className="filter-row" role="group" aria-label="Filter and sort options">
          <SlidersHorizontal size={16} style={{ color: 'var(--ink-soft)' }} aria-hidden="true" />
          <label htmlFor="sort-select" className="muted">Sort by</label>
          <select
            id="sort-select"
            value={sort}
            onChange={e => { setPage(0); setSort(e.target.value); }}
          >
            <option value="soon">Upcoming first</option>
            <option value="newest">Newest added</option>
          </select>

          {data && (
            <span className="count-badge" aria-live="polite" aria-atomic="true">
              {data.total} event{data.total !== 1 ? 's' : ''}
            </span>
          )}
        </div>

        <div className="section-head">
          <h2 id="events-heading">
            {cat ? cat : 'All'} Events
            {data && <span className="muted" style={{ fontSize: 18, fontWeight: 500 }}> ({data.total})</span>}
          </h2>
        </div>

        {/* Error */}
        {err && (
          <div className="alert-box" role="alert" aria-live="assertive">
            <div className="alert-box-content">
              <span className="alert-icon" aria-hidden="true">⚠️</span>
              <span>{err}</span>
            </div>
            <button className="btn btn-ghost btn-sm" onClick={() => load()}>Retry</button>
          </div>
        )}

        {/* Skeleton */}
        {loading && !err && (
          <div className="grid" aria-label="Loading events…" aria-busy="true">
            {[1, 2, 3, 4, 5, 6].map(i => <div key={i} className="skel" aria-hidden="true" />)}
          </div>
        )}

        {/* No results */}
        {data && data.items.length === 0 && (
          <div className="empty-state" role="status">
            <div className="empty-state-icon" aria-hidden="true">🔍</div>
            <h3>No events found</h3>
            <p>Try a different keyword, category, or clear your filters.</p>
            <button
              className="btn btn-ghost"
              onClick={() => { setQ(''); setCat(''); setPage(0); load(''); }}
            >
              Clear filters
            </button>
          </div>
        )}

        {/* Grid */}
        {data && data.items.length > 0 && (
          <div className="grid" role="list" aria-label={`${cat || 'All'} events`}>
            {data.items.map(e => <EventCard key={e.id} e={e} />)}
          </div>
        )}

        {/* Pagination */}
        {data && data.totalPages > 1 && (
          <nav className="pager" aria-label="Pagination">
            <button
              className="btn btn-ghost btn-sm"
              disabled={page === 0}
              onClick={() => setPage(page - 1)}
              aria-label="Previous page"
            >
              <ChevronLeft size={16} /> Previous
            </button>
            <span className="pager-info" aria-current="page">
              Page {page + 1} of {data.totalPages}
            </span>
            <button
              className="btn btn-ghost btn-sm"
              disabled={page + 1 >= data.totalPages}
              onClick={() => setPage(page + 1)}
              aria-label="Next page"
            >
              Next <ChevronRight size={16} />
            </button>
          </nav>
        )}
      </section>
    </>
  );
}
