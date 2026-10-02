import { useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router'
import { api, useApi, type Schedule } from '../api'
import { useUser } from '../auth'
import { formatDate, formatRupiah, formatTime, minutesUntilShow } from '../format'
import { ErrorMessage } from '../ui'

// ponytail: the API only lists free seats, so the layout mirrors the V1 seed (every studio A1-E10);
// add a "all seats of a studio" endpoint if studios ever get different layouts
const ROWS = ['A', 'B', 'C', 'D', 'E']
const NUMBERS = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
const MAX_SEATS = 10 // same cap as the backend's TicketRequest

export default function SeatsPage() {
  const { scheduleId } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const user = useUser()
  const schedule = useApi<Schedule>(`/schedules/${scheduleId}`)
  const freeSeats = useApi<string[]>(`/schedules/${scheduleId}/seats`)
  const [selected, setSelected] = useState<string[]>([])
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  if (schedule.error) {
    return (
      <section>
        <ErrorMessage error={schedule.error} />
        <Link to="/">‹ Kembali ke daftar film</Link>
      </section>
    )
  }
  if (!schedule.data || !freeSeats.data) {
    return <p className="muted">Memuat…</p>
  }

  const show = schedule.data
  const free = new Set(freeSeats.data)
  const started = minutesUntilShow(show.filmDate, show.filmStartTime) <= 0
  const full = selected.length >= MAX_SEATS

  function toggle(seat: string) {
    setSelected((current) => (current.includes(seat) ? current.filter((s) => s !== seat) : [...current, seat]))
  }

  async function book() {
    if (!user) {
      navigate('/login', { state: { from: location.pathname } })
      return
    }
    setBusy(true)
    setError(null)
    try {
      await api('/tickets', {
        method: 'POST',
        body: JSON.stringify({ scheduleId: show.scheduleId, seatsCodes: selected }),
      })
      navigate('/tickets', { state: { booked: selected.length } })
    } catch (e) {
      // usually someone else just took one of the seats: show it and refresh the map
      setError((e as Error).message)
      setSelected([])
      freeSeats.reload()
      setBusy(false)
    }
  }

  return (
    <section>
      <Link to={`/films/${show.film.filmId}`}>‹ {show.film.filmName}</Link>
      <h1>Pilih kursi</h1>
      <p className="muted">
        {formatDate(show.filmDate)} · {formatTime(show.filmStartTime)}–{formatTime(show.filmEndTime)} · Studio{' '}
        {show.studioName} · {formatRupiah(show.ticketPrice)} per kursi
      </p>

      {started ? (
        <p className="notice">Jadwal ini sudah mulai, tiket tidak bisa dipesan lagi.</p>
      ) : (
        <>
          <div className="seat-map">
            <div className="screen">Layar</div>
            {ROWS.map((row) => (
              <div className="seat-row" key={row}>
                <span className="seat-label">{row}</span>
                {NUMBERS.map((number) => {
                  const seat = `${row}${number}`
                  const isSelected = selected.includes(seat)
                  return (
                    <button
                      key={seat}
                      type="button"
                      className={free.has(seat) ? 'seat' : 'seat taken'}
                      aria-label={`Kursi ${seat}${free.has(seat) ? '' : ', sudah terisi'}`}
                      aria-pressed={isSelected}
                      disabled={!free.has(seat) || (full && !isSelected)}
                      onClick={() => toggle(seat)}
                    >
                      {number}
                    </button>
                  )
                })}
              </div>
            ))}
          </div>

          <ul className="seat-legend" aria-label="Keterangan">
            <li>
              <span className="seat sample" /> Kosong
            </li>
            <li>
              <span className="seat sample selected" /> Dipilih
            </li>
            <li>
              <span className="seat sample taken" /> Terisi
            </li>
          </ul>

          <div className="checkout">
            <div>
              <strong>{selected.length ? selected.join(', ') : 'Belum ada kursi dipilih'}</strong>
              <div className="muted">
                {selected.length} kursi · {formatRupiah(selected.length * show.ticketPrice)}
                {full && ` · maksimal ${MAX_SEATS} kursi per pesanan`}
              </div>
            </div>
            <button type="button" disabled={busy || selected.length === 0} onClick={book}>
              {busy ? 'Memproses…' : user ? 'Pesan tiket' : 'Masuk untuk memesan'}
            </button>
          </div>
          <ErrorMessage error={error ?? freeSeats.error} />
        </>
      )}
    </section>
  )
}
