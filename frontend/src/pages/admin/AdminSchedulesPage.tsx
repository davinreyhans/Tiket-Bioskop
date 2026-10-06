import { useState, type FormEvent } from 'react'
import { api, describeError, useApi, type Film, type Page, type Schedule } from '../../api'
import { formatDate, formatRupiah, formatTime } from '../../format'
import { ErrorMessage, Pager } from '../../ui'

// ponytail: studios mirror the V1 seed (A-C), like the seat map; add a studios endpoint if they change
const STUDIOS = ['A', 'B', 'C']

export default function AdminSchedulesPage() {
  const [filmFilter, setFilmFilter] = useState('')
  const [page, setPage] = useState(0)
  // ponytail: the picker loads at most 100 films (the API's max page size); switch to a search box past that
  const films = useApi<Page<Film>>('/films?size=100&sort=filmName')
  const schedules = useApi<Page<Schedule>>(`/schedules?page=${page}${filmFilter ? `&filmId=${filmFilter}` : ''}`)
  const [editing, setEditing] = useState<Schedule | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function run(action: () => Promise<unknown>) {
    setBusy(true)
    setError(null)
    try {
      await action()
      schedules.reload()
      return true
    } catch (e) {
      setError(describeError(e))
      return false
    } finally {
      setBusy(false)
    }
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const formElement = event.currentTarget // React clears currentTarget once the handler awaits
    const form = new FormData(formElement)
    const body = JSON.stringify({
      filmId: Number(form.get('filmId')),
      studioName: form.get('studioName'),
      filmDate: form.get('filmDate'),
      filmStartTime: form.get('filmStartTime'),
      filmEndTime: form.get('filmEndTime'),
      ticketPrice: Number(form.get('ticketPrice')),
    })
    const saved = await run(() =>
      editing
        ? api(`/schedules/${editing.scheduleId}`, { method: 'PUT', body })
        : api('/schedules', { method: 'POST', body }),
    )
    if (saved) {
      setEditing(null)
      if (!editing) {
        formElement.reset()
      }
    }
  }

  function remove(schedule: Schedule) {
    const label = `${schedule.film.filmName}, ${formatDate(schedule.filmDate)} ${formatTime(schedule.filmStartTime)}`
    if (window.confirm(`Hapus jadwal ${label}?`)) {
      run(() => api(`/schedules/${schedule.scheduleId}`, { method: 'DELETE' }))
    }
  }

  return (
    <>
      <h2>{editing ? `Edit jadwal #${editing.scheduleId}` : 'Tambah jadwal'}</h2>
      <form className="admin-form" onSubmit={onSubmit} key={editing?.scheduleId ?? 'new'}>
        <label className="grow">
          Film
          <select name="filmId" required defaultValue={editing?.film.filmId ?? ''}>
            <option value="" disabled>
              Pilih film…
            </option>
            {films.data?.content.map((film) => (
              <option key={film.filmId} value={film.filmId}>
                {film.filmName}
              </option>
            ))}
          </select>
        </label>
        <label>
          Studio
          <select name="studioName" required defaultValue={editing?.studioName ?? 'A'}>
            {STUDIOS.map((studio) => (
              <option key={studio}>{studio}</option>
            ))}
          </select>
        </label>
        <label>
          Tanggal
          <input name="filmDate" type="date" required defaultValue={editing?.filmDate} />
        </label>
        <label>
          Mulai
          <input name="filmStartTime" type="time" required defaultValue={editing ? formatTime(editing.filmStartTime) : undefined} />
        </label>
        <label>
          Selesai
          <input name="filmEndTime" type="time" required defaultValue={editing ? formatTime(editing.filmEndTime) : undefined} />
        </label>
        <label>
          Harga (Rp)
          <input name="ticketPrice" type="number" required min={0} step={1000} defaultValue={editing?.ticketPrice ?? 50000} />
        </label>
        <div className="actions">
          <button type="submit" disabled={busy}>
            {editing ? 'Simpan' : 'Tambah'}
          </button>
          {editing && (
            <button type="button" className="secondary" onClick={() => setEditing(null)}>
              Batal
            </button>
          )}
        </div>
        <small className="muted full">Jam selesai lebih awal dari jam mulai = selesai besoknya (maks 6 jam).</small>
      </form>

      <ErrorMessage error={error ?? schedules.error ?? films.error} />

      <h2>Semua jadwal</h2>
      <div className="toolbar">
        <label>
          Film{' '}
          <select
            value={filmFilter}
            onChange={(event) => {
              setFilmFilter(event.target.value)
              setPage(0)
            }}
          >
            <option value="">Semua film</option>
            {films.data?.content.map((film) => (
              <option key={film.filmId} value={film.filmId}>
                {film.filmName}
              </option>
            ))}
          </select>
        </label>
      </div>

      {schedules.data && schedules.data.content.length === 0 && <p className="muted">Belum ada jadwal.</p>}
      {schedules.data && schedules.data.content.length > 0 && (
        <table aria-busy={schedules.loading}>
          <thead>
            <tr>
              <th>Film</th>
              <th>Tanggal</th>
              <th>Jam</th>
              <th>Studio</th>
              <th>Harga</th>
              <th>
                <span className="visually-hidden">Aksi</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {schedules.data.content.map((schedule) => (
              <tr key={schedule.scheduleId}>
                <td>{schedule.film.filmName}</td>
                <td>{formatDate(schedule.filmDate)}</td>
                <td>
                  {formatTime(schedule.filmStartTime)}–{formatTime(schedule.filmEndTime)}
                </td>
                <td>{schedule.studioName}</td>
                <td>{formatRupiah(schedule.ticketPrice)}</td>
                <td className="row-actions">
                  <button type="button" className="secondary" disabled={busy} onClick={() => setEditing(schedule)}>
                    Edit
                  </button>
                  <button type="button" className="secondary" disabled={busy} onClick={() => remove(schedule)}>
                    Hapus
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {schedules.data && <Pager page={schedules.data.page} onChange={setPage} />}
    </>
  )
}
