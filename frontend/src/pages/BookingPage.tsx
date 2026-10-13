import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { api, ApiError, type Booking } from '../api'
import { formatDate, formatRupiah, formatTime } from '../format'
import { ErrorMessage } from '../ui'

// "2026-10-12T23:41:13.395143" (cinema time, no zone) -> ms; only differences between two of these matter
function cinemaMs(dateTime: string) {
  return Date.parse(`${dateTime.slice(0, 19)}Z`)
}

// 299 -> "4:59"
function formatCountdown(seconds: number) {
  return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`
}

export default function BookingPage() {
  const { bookingId } = useParams()
  const navigate = useNavigate()
  // deadline: this device's Date.now() at which the reservation ends (expiresAt - serverTime from receipt)
  const [loaded, setLoaded] = useState<{ booking?: Booking; receivedAt?: number; deadline?: number; error?: Error }>({})
  const [now, setNow] = useState(() => Date.now())
  const [busy, setBusy] = useState<'pay' | 'cancel' | null>(null)
  const [error, setError] = useState<string | null>(null)
  // the server said it's too late (409), or the expired reservation is already gone (404)
  const [expiredOnServer, setExpiredOnServer] = useState(false)

  useEffect(() => {
    let cancelled = false
    api<Booking>(`/bookings/${bookingId}`).then(
      (booking) => {
        const receivedAt = Date.now()
        if (!cancelled) {
          setLoaded({ booking, receivedAt, deadline: receivedAt + cinemaMs(booking.expiresAt) - cinemaMs(booking.serverTime) })
        }
      },
      (e: Error) => !cancelled && setLoaded({ error: e }),
    )
    return () => {
      cancelled = true
    }
  }, [bookingId])

  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 1000)
    return () => clearInterval(timer)
  }, [])

  const { booking, receivedAt, deadline } = loaded
  if (!booking || receivedAt === undefined || deadline === undefined) {
    return loaded.error ? (
      <section>
        <ErrorMessage error={loaded.error} />
        <Link to="/">‹ Kembali ke daftar film</Link>
      </section>
    ) : (
      <p className="muted">Memuat…</p>
    )
  }

  const show = booking.schedule
  const seats = booking.tickets.map((ticket) => ticket.seatsCode)
  const total = seats.length * show.ticketPrice
  // `now` only ticks once a second and may still be from before the booking arrived
  const secondsLeft = Math.max(0, Math.ceil((deadline - Math.max(now, receivedAt)) / 1000))
  const expired = booking.status === 'RESERVED' && (secondsLeft === 0 || expiredOnServer)

  async function pay() {
    setBusy('pay')
    setError(null)
    try {
      await api(`/bookings/${booking!.bookingId}/pay`, { method: 'POST' })
      navigate('/tickets', { state: { booked: seats.length, scheduleId: show.scheduleId } })
    } catch (e) {
      if (e instanceof ApiError && (e.status === 409 || e.status === 404)) {
        setExpiredOnServer(true)
      } else {
        setError((e as Error).message)
      }
      setBusy(null)
    }
  }

  async function cancel() {
    if (!window.confirm(`Batalkan pesanan kursi ${seats.join(', ')}? Kursinya langsung dilepas untuk orang lain.`)) {
      return
    }
    setBusy('cancel')
    setError(null)
    try {
      await api(`/bookings/${booking!.bookingId}`, { method: 'DELETE' })
      navigate(`/schedules/${show.scheduleId}`, { replace: true })
    } catch (e) {
      setError((e as Error).message)
      setBusy(null)
    }
  }

  return (
    <section className="booking">
      <Link to={`/films/${show.film.filmId}`} className="back">
        ‹ {show.film.filmName}
      </Link>
      <h1>Pembayaran</h1>

      {booking.status === 'PAID' ? (
        <p className="notice success">
          Pesanan ini sudah dibayar. <Link to="/tickets">Lihat tiket saya</Link>
        </p>
      ) : expired ? (
        <p className="notice" role="alert">
          Waktu pembayaran habis, kursi dilepas.{' '}
          <Link to={`/schedules/${show.scheduleId}?seats=${seats.join(',')}`}>Pilih kursi lagi</Link>
        </p>
      ) : (
        // role="timer": screen readers don't announce every second, only when asked
        <p className="notice countdown" role="timer">
          Selesaikan pembayaran dalam <strong>{formatCountdown(secondsLeft)}</strong>. Setelah itu kursinya dilepas.
        </p>
      )}

      <article className="ticket">
        <header>
          <h2>{show.film.filmName}</h2>
          <p className="muted">
            {formatDate(show.filmDate)} · {formatTime(show.filmStartTime)}–{formatTime(show.filmEndTime)} · Studio{' '}
            {show.studioName}
          </p>
        </header>
        <ul className="ticket-seats" aria-label="Kursi">
          {seats.map((seat) => (
            <li key={seat}>
              <span className="seat-code">{seat}</span>
            </li>
          ))}
        </ul>
        <footer>
          <p className="ticket-total">
            <strong>{formatRupiah(total)}</strong>{' '}
            <span className="muted">
              {seats.length} × {formatRupiah(show.ticketPrice)}
            </span>
          </p>
        </footer>
      </article>

      {booking.status === 'RESERVED' && (
        <>
          <div className="booking-actions">
            <button type="button" disabled={busy !== null || expired} onClick={pay}>
              {busy === 'pay' ? 'Membayar…' : `Bayar ${formatRupiah(total)}`}
            </button>
            {/* an expired reservation has nothing left to cancel */}
            {!expired && (
              <button type="button" className="secondary" disabled={busy !== null} onClick={cancel}>
                {busy === 'cancel' ? 'Membatalkan…' : 'Batalkan'}
              </button>
            )}
          </div>
          <ErrorMessage error={error} />
          <p className="muted">Pembayaran simulasi: tidak ada uang yang ditarik.</p>
        </>
      )}
    </section>
  )
}
