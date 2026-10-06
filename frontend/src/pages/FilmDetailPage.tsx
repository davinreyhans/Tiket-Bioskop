import { Link, useParams, useSearchParams } from 'react-router'
import { useApi, type Film, type Page, type Schedule } from '../api'
import { formatDate, formatRupiah, formatTime, minutesUntilShow } from '../format'
import { ErrorMessage, Pager } from '../ui'

export default function FilmDetailPage() {
  const { filmId } = useParams()
  const [params, setParams] = useSearchParams()
  const date = params.get('date') ?? ''
  const page = Number(params.get('page') ?? 0)

  const film = useApi<Film>(`/films/${filmId}`)
  const schedules = useApi<Page<Schedule>>(
    `/schedules?filmId=${filmId}${date ? `&date=${date}` : ''}&page=${page}`,
  )

  if (film.error) {
    return (
      <section>
        <ErrorMessage error={film.error} />
        <Link to="/">‹ Kembali ke daftar film</Link>
      </section>
    )
  }

  return (
    <section>
      <Link to="/">‹ Semua film</Link>
      {film.data && (
        <header className="film-header">
          <h1>{film.data.filmName}</h1>
          <span className="muted">{film.data.filmCode}</span>
          <span className={film.data.isShowing ? 'badge' : 'badge off'}>
            {film.data.isShowing ? 'Sedang tayang' : 'Tidak tayang'}
          </span>
        </header>
      )}

      <h2>Jadwal</h2>
      <div className="toolbar">
        <label>
          Tanggal{' '}
          <input type="date" value={date} onChange={(event) => setParams(event.target.value ? { date: event.target.value } : {})} />
        </label>
        {date && (
          <button type="button" className="secondary" onClick={() => setParams({})}>
            Semua tanggal
          </button>
        )}
      </div>

      <ErrorMessage error={schedules.error} />
      {schedules.loading && !schedules.data && <p className="muted">Memuat…</p>}
      {schedules.data && schedules.data.content.length === 0 && (
        <p className="muted">Belum ada jadwal{date ? ' di tanggal ini' : ''}.</p>
      )}

      {schedules.data && schedules.data.content.length > 0 && (
        <table aria-busy={schedules.loading}>
          <thead>
            <tr>
              <th>Tanggal</th>
              <th>Jam</th>
              <th>Studio</th>
              <th>Harga</th>
              <th><span className="visually-hidden">Aksi</span></th>
            </tr>
          </thead>
          <tbody>
            {schedules.data.content.map((schedule) => (
              <tr key={schedule.scheduleId}>
                <td>{formatDate(schedule.filmDate)}</td>
                <td>
                  {formatTime(schedule.filmStartTime)}–{formatTime(schedule.filmEndTime)}
                  {/* an end time at or before the start time means the show ends the next day */}
                  {schedule.filmEndTime <= schedule.filmStartTime && <span className="muted"> (+1 hari)</span>}
                </td>
                <td>{schedule.studioName}</td>
                <td>{formatRupiah(schedule.ticketPrice)}</td>
                <td>
                  {minutesUntilShow(schedule.filmDate, schedule.filmStartTime) > 0 ? (
                    <Link to={`/schedules/${schedule.scheduleId}`}>Pilih kursi</Link>
                  ) : (
                    <span className="muted">Sudah mulai</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {schedules.data && (
        <Pager
          page={schedules.data.page}
          onChange={(next) => setParams({ ...(date ? { date } : {}), ...(next ? { page: String(next) } : {}) })}
        />
      )}
    </section>
  )
}
