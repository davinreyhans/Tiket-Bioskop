import { Link, useParams, useSearchParams } from 'react-router'
import { useApi, type Film, type Page, type Schedule } from '../api'
import { cinemaToday, formatDate, formatDay, formatRupiah, formatTime, minutesUntilShow } from '../format'
import { ErrorMessage } from '../ui'

export default function FilmDetailPage() {
  const { filmId } = useParams()
  const [params, setParams] = useSearchParams()

  const film = useApi<Film>(`/films/${filmId}`)
  // ponytail: the next 100 shows of one film, no paging; page through `from` if a film ever has more
  const schedules = useApi<Page<Schedule>>(`/schedules?filmId=${filmId}&from=${cinemaToday()}&size=100`)

  if (film.error) {
    return (
      <section>
        <ErrorMessage error={film.error} />
        <Link to="/">‹ Kembali ke daftar film</Link>
      </section>
    )
  }

  // only dates that still have a show to book, in order
  const byDate = new Map<string, Schedule[]>()
  schedules.data?.content
    .filter((show) => minutesUntilShow(show.filmDate, show.filmStartTime) > 0)
    .forEach((show) => byDate.set(show.filmDate, [...(byDate.get(show.filmDate) ?? []), show]))
  const dates = [...byDate.keys()]
  const date = byDate.has(params.get('date') ?? '') ? params.get('date')! : dates[0]

  return (
    <section>
      <Link to="/" className="back">‹ Semua film</Link>
      {film.data && (
        <header className="film-header">
          <h1>{film.data.filmName}</h1>
          <span className={film.data.isShowing ? 'badge' : 'badge off'}>
            {film.data.isShowing ? 'Sedang tayang' : 'Tidak tayang'}
          </span>
        </header>
      )}

      <h2>Jadwal</h2>
      <ErrorMessage error={schedules.error} />
      {schedules.loading && !schedules.data && <p className="muted">Memuat…</p>}
      {schedules.data && dates.length === 0 && <p className="muted">Belum ada jadwal mendatang.</p>}

      {date && (
        <>
          <div className="tabs" role="group" aria-label="Tanggal">
            {dates.map((day) => (
              <button
                key={day}
                type="button"
                aria-pressed={day === date}
                title={formatDate(day)}
                onClick={() => setParams({ date: day }, { replace: true })}
              >
                {formatDay(day)}
              </button>
            ))}
          </div>

          <p className="muted">{formatDate(date)}</p>
          <ul className="showtimes">
            {byDate.get(date)!.map((show) => (
              <li key={show.scheduleId}>
                <Link to={`/schedules/${show.scheduleId}`} className="card">
                  <strong className="showtime">{formatTime(show.filmStartTime)}</strong>
                  <span className="muted">
                    s.d. {formatTime(show.filmEndTime)}
                    {/* an end time at or before the start time means the show ends the next day */}
                    {show.filmEndTime <= show.filmStartTime && ' (+1 hari)'}
                  </span>
                  <span>
                    Studio {show.studioName} · {formatRupiah(show.ticketPrice)}
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        </>
      )}
    </section>
  )
}
