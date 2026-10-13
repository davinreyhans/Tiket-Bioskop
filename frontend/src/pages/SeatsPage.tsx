import { useState, type KeyboardEvent } from 'react'
import { Link, useLocation, useNavigate, useParams, useSearchParams } from 'react-router'
import { api, useApi, type Schedule } from '../api'
import { useUser } from '../auth'
import {
  CANCEL_DEADLINE_MINUTES,
  formatCancelDeadline,
  formatDate,
  formatRupiah,
  formatTime,
  minutesUntilShow,
} from '../format'
import { ErrorMessage } from '../ui'

// ponytail: the API only lists free seats, so the layout mirrors the V1 seed (every studio A1-E10);
// add a "all seats of a studio" endpoint if studios ever get different layouts
const ROWS = ['A', 'B', 'C', 'D', 'E']
const NUMBERS = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
const MAX_SEATS = 10 // same cap as the backend's TicketRequest
const SEAT_CODE = /^[A-E]([1-9]|10)$/

export default function SeatsPage() {
  const { scheduleId } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const user = useUser()
  const schedule = useApi<Schedule>(`/schedules/${scheduleId}`)
  const freeSeats = useApi<string[]>(`/schedules/${scheduleId}/seats`)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  // the seat map is one tab stop; arrow keys move between seats (roving tabindex)
  const [focusSeat, setFocusSeat] = useState('A1')

  // The selection lives in the URL (?seats=A1,A2), so it survives logging in, registering and refreshing
  const [params, setParams] = useSearchParams()
  const requested = [...new Set((params.get('seats') ?? '').split(',').filter((code) => SEAT_CODE.test(code)))]
    .slice(0, MAX_SEATS)

  function setSelected(seats: string[]) {
    setParams(seats.length ? { seats: seats.join(',') } : {}, { replace: true })
  }

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
  // seats kept in the URL may have been taken by someone else since they were picked
  const selected = requested.filter((seat) => free.has(seat))
  const lost = requested.filter((seat) => !free.has(seat))
  const minutesLeft = minutesUntilShow(show.filmDate, show.filmStartTime)
  const started = minutesLeft <= 0
  const full = selected.length >= MAX_SEATS

  function toggle(seat: string) {
    if (!free.has(seat) || (full && !selected.includes(seat))) return
    setError(null)
    setSelected(selected.includes(seat) ? selected.filter((s) => s !== seat) : [...selected, seat])
  }

  function move(event: KeyboardEvent<HTMLDivElement>) {
    const steps: Record<string, [number, number]> = { ArrowUp: [-1, 0], ArrowDown: [1, 0], ArrowLeft: [0, -1], ArrowRight: [0, 1] }
    const step = steps[event.key]
    if (!step) return
    event.preventDefault()
    // read the position from the focused seat: state lags behind when keys repeat quickly
    const current = (event.target as HTMLElement).dataset.seat ?? focusSeat
    const row = Math.min(Math.max(ROWS.indexOf(current[0]) + step[0], 0), ROWS.length - 1)
    const number = Math.min(Math.max(Number(current.slice(1)) + step[1], 1), NUMBERS.length)
    const next = `${ROWS[row]}${number}`
    setFocusSeat(next)
    event.currentTarget.querySelector<HTMLElement>(`[data-seat="${next}"]`)?.focus()
  }

  async function book() {
    if (!user) {
      // come back to this exact selection after logging in (or registering)
      navigate('/login', { state: { from: location.pathname + location.search } })
      return
    }
    setBusy(true)
    setError(null)
    try {
      // the seats are now held for 5 minutes; payment happens on the booking page
      const booking = await api<{ bookingId: number }>('/bookings', {
        method: 'POST',
        body: JSON.stringify({ scheduleId: show.scheduleId, seatsCodes: selected }),
      })
      navigate(`/bookings/${booking.bookingId}`)
    } catch (e) {
      // usually someone else just took one of the seats: keep the seats that are still free
      setError((e as Error).message)
      const stillFree = await api<string[]>(`/schedules/${show.scheduleId}/seats`).catch(() => null)
      if (stillFree) {
        setSelected(selected.filter((seat) => stillFree.includes(seat)))
      }
      freeSeats.reload()
      setBusy(false)
    }
  }

  return (
    <section>
      <Link to={`/films/${show.film.filmId}`} className="back">
        ‹ {show.film.filmName}
      </Link>
      <h1>Pilih kursi</h1>
      <p className="muted">
        {formatDate(show.filmDate)} · {formatTime(show.filmStartTime)}–{formatTime(show.filmEndTime)} · Studio{' '}
        {show.studioName} · {formatRupiah(show.ticketPrice)} per kursi
      </p>

      {started ? (
        <p className="notice">Jadwal ini sudah mulai, tiket tidak bisa dipesan lagi.</p>
      ) : (
        <>
          <div className="seat-map" role="group" aria-label="Peta kursi, pindah dengan tombol panah" onKeyDown={move}>
            <div className="screen">Layar</div>
            {ROWS.map((row) => (
              <div className="seat-row" key={row}>
                <span className="seat-label">{row}</span>
                {NUMBERS.map((number) => {
                  const seat = `${row}${number}`
                  const isSelected = selected.includes(seat)
                  // aria-disabled, not disabled: taken seats stay reachable with the arrow keys
                  return (
                    <button
                      key={seat}
                      type="button"
                      data-seat={seat}
                      tabIndex={seat === focusSeat ? 0 : -1}
                      className={free.has(seat) ? 'seat' : 'seat taken'}
                      aria-label={`Kursi ${seat}${free.has(seat) ? '' : ', sudah terisi'}`}
                      aria-pressed={isSelected}
                      aria-disabled={!free.has(seat) || (full && !isSelected)}
                      onFocus={() => setFocusSeat(seat)}
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

          {lost.length > 0 && !error && (
            <p className="notice">
              Kursi {lost.join(', ')} sudah dipesan orang lain dan dilepas dari pilihanmu.
            </p>
          )}

          <div className="checkout">
            <div className="checkout-summary">
              <strong>{selected.length ? `Kursi ${selected.join(', ')}` : 'Belum ada kursi dipilih'}</strong>
              <div className="muted checkout-show">
                {show.film.filmName} · {formatDate(show.filmDate)}, {formatTime(show.filmStartTime)} · Studio{' '}
                {show.studioName}
              </div>
              {selected.length > 0 && (
                <div className="muted">
                  {selected.length} × {formatRupiah(show.ticketPrice)}
                  {full && ` · maksimal ${MAX_SEATS} kursi per pesanan`}
                </div>
              )}
              {selected.length > 0 && <div className="muted">Kursi ditahan 5 menit untuk pembayaran</div>}
              <div className="muted">
                {minutesLeft > CANCEL_DEADLINE_MINUTES
                  ? `Bisa dibatalkan sampai ${formatCancelDeadline(show.filmDate, show.filmStartTime)}`
                  : 'Tidak bisa dibatalkan lagi: kurang dari 2 jam sebelum tayang'}
              </div>
            </div>
            <button type="button" disabled={busy || selected.length === 0} onClick={book}>
              {busy
                ? 'Menahan kursi…'
                : !user
                  ? 'Masuk untuk memesan'
                  : selected.length
                    ? `Lanjut ke pembayaran · ${formatRupiah(selected.length * show.ticketPrice)}`
                    : 'Pilih kursi dulu'}
            </button>
          </div>
          <ErrorMessage error={error ?? freeSeats.error} />
        </>
      )}
    </section>
  )
}
