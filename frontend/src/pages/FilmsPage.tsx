import { Link, useSearchParams } from 'react-router'
import { useApi, type Film, type Page } from '../api'
import { ErrorMessage, Pager } from '../ui'

const FILTERS = [
  { value: 'true', label: 'Sedang tayang' },
  { value: 'false', label: 'Tidak tayang' },
  { value: 'all', label: 'Semua' },
]

// Filter, search and page live in the URL, so refresh and the back button keep them
export default function FilmsPage() {
  const [params, setParams] = useSearchParams()
  const name = params.get('name') ?? ''
  const showing = params.get('showing') ?? 'true'
  const page = Number(params.get('page') ?? 0)

  // the search endpoint has no "showing" filter, so searching covers every film
  const path = name
    ? `/films/search?name=${encodeURIComponent(name)}&page=${page}`
    : `/films?${showing === 'all' ? '' : `showing=${showing}&`}page=${page}`
  const { data, error, loading } = useApi<Page<Film>>(path)

  function update(changes: Record<string, string>) {
    const next = new URLSearchParams(params)
    Object.entries(changes).forEach(([key, value]) => (value ? next.set(key, value) : next.delete(key)))
    setParams(next)
  }

  return (
    <section>
      <h1>Film</h1>

      <form
        className="toolbar"
        role="search"
        onSubmit={(event) => {
          event.preventDefault()
          const value = new FormData(event.currentTarget).get('name') as string
          update({ name: value.trim(), page: '' })
        }}
      >
        <input name="name" type="search" placeholder="Cari judul film…" defaultValue={name} key={name} />
        <button type="submit">Cari</button>
        {name && (
          <button type="button" className="secondary" onClick={() => update({ name: '', page: '' })}>
            Hapus pencarian
          </button>
        )}
      </form>

      {name ? (
        <p className="muted">Hasil pencarian "{name}" dari semua film.</p>
      ) : (
        <div className="tabs" role="group" aria-label="Status tayang">
          {FILTERS.map((filter) => (
            <button
              key={filter.value}
              type="button"
              aria-pressed={showing === filter.value}
              onClick={() => update({ showing: filter.value === 'true' ? '' : filter.value, page: '' })}
            >
              {filter.label}
            </button>
          ))}
        </div>
      )}

      <ErrorMessage error={error} />
      {loading && !data && <p className="muted">Memuat…</p>}
      {data && data.content.length === 0 && <p className="muted">Tidak ada film.</p>}

      <ul className="film-grid" aria-busy={loading}>
        {data?.content.map((film) => (
          <li key={film.filmId}>
            <Link to={`/films/${film.filmId}`} className="card">
              <strong>{film.filmName}</strong>
              <span className="muted">{film.filmCode}</span>
              <span className={film.isShowing ? 'badge' : 'badge off'}>
                {film.isShowing ? 'Sedang tayang' : 'Tidak tayang'}
              </span>
            </Link>
          </li>
        ))}
      </ul>

      {data && <Pager page={data.page} onChange={(next) => update({ page: next ? String(next) : '' })} />}
    </section>
  )
}
