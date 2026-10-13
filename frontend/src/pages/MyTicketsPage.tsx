import { useEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'
import { api, useApi, type Booking, type Schedule, type Ticket } from '../api'
import {
  CANCEL_DEADLINE_MINUTES,
  formatDate,
  formatRupiah,
  formatStartsIn,
  formatTime,
  minutesUntilShow,
} from '../format'
import { ErrorMessage } from '../ui'

export default function MyTicketsPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const tickets = useApi<Ticket[]>('/tickets/me')
  // reserved but not paid yet; they drop out of this list by themselves after 5 minutes
  const reservations = useApi<Booking[]>('/bookings/me')
  const [error, setError] = useState<string | null>(null)
  const [cancelling, setCancelling] = useState<number[]>([])
  const [cancelled, setCancelled] = useState<string | null>(null)
  // read the "just booked" note once, then drop it from history so a reload doesn't show it again
  const [booked] = useState(() => location.state as { booked?: number; scheduleId?: number } | null)
  useEffect(() => {
    if (location.state) {
      navigate(location.pathname, { replace: true, state: null })
    }
  }, [location.pathname, location.state, navigate])

  // ponytail: one DELETE per seat, in order; a failure halfway keeps the seats cancelled so far (the reload
  // shows what's left). Add a "cancel booking" endpoint if this needs to be all-or-nothing
  async function cancel(seats: Ticket[]) {
    const film = seats[0].schedule.film.filmName
    const codes = seats.map((ticket) => ticket.seatsCode).join(', ')
    if (!window.confirm(`Batalkan tiket kursi ${codes} untuk ${film}? Kursinya akan dilepas untuk orang lain.`)) {
      return
    }
    setCancelling(seats.map((ticket) => ticket.ticketId))
    setError(null)
    setCancelled(null)
    try {
      for (const ticket of seats) {
        await api(`/tickets/${ticket.ticketId}`, { method: 'DELETE' })
      }
      setCancelled(`Tiket kursi ${codes} untuk ${film} dibatalkan.`)
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setCancelling([])
      tickets.reload()
    }
  }

  const justBooked = (schedule: Schedule) => booked?.scheduleId === schedule.scheduleId
  // bring the just-booked show into view once, not again on every re-render (e.g. after a cancel)
  const scrolled = useRef(false)
  const scrollOnce = (node: HTMLElement | null) => {
    if (node && !scrolled.current) {
      scrolled.current = true
      node.scrollIntoView({ block: 'nearest' })
    }
  }

  // one card per show: upcoming shows first (soonest on top), then past shows (most recent on top)
  const shows = new Map<number, { schedule: Schedule; tickets: Ticket[] }>()
  tickets.data?.forEach((ticket) => {
    const show = shows.get(ticket.schedule.scheduleId) ?? { schedule: ticket.schedule, tickets: [] }
    show.tickets.push(ticket)
    shows.set(ticket.schedule.scheduleId, show)
  })
  const ordered = [...shows.values()]
    .map((show) => ({ ...show, minutesLeft: minutesUntilShow(show.schedule.filmDate, show.schedule.filmStartTime) }))
    .sort((a, b) => {
      const aPast = a.minutesLeft <= 0
      const bPast = b.minutesLeft <= 0
      if (aPast !== bPast) {
        return aPast ? 1 : -1
      }
      return aPast ? b.minutesLeft - a.minutesLeft : a.minutesLeft - b.minutesLeft
    })

  return (
    <section>
      <h1>Tiket saya</h1>
      {booked?.booked && <BookedNotice count={booked.booked} show={shows.get(booked.scheduleId ?? -1)?.schedule} />}
      {cancelled && (
        <p className="notice success" role="status">
          {cancelled}
        </p>
      )}
      <ErrorMessage error={error ?? tickets.error} />
      {tickets.loading && !tickets.data && <p className="muted">Memuat…</p>}
      {tickets.data?.length === 0 && reservations.data?.length === 0 && (
        <p className="muted">
          Belum ada tiket. <Link to="/">Lihat film yang sedang tayang</Link>
        </p>
      )}

      <div className="stack">
        {reservations.data?.map((booking) => {
          const show = booking.schedule
          const seats = booking.tickets.map((ticket) => ticket.seatsCode)
          return (
            <article key={`booking-${booking.bookingId}`} className="ticket">
              <header>
                <h2>
                  <Link to={`/films/${show.film.filmId}`}>{show.film.filmName}</Link>
                </h2>
                <span className="badge">Menunggu pembayaran</span>
                <p className="muted">
                  {formatDate(show.filmDate)} · {formatTime(show.filmStartTime)}–{formatTime(show.filmEndTime)} ·
                  Studio {show.studioName}
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
                  <strong>{formatRupiah(seats.length * show.ticketPrice)}</strong>{' '}
                  <span className="muted">bayar sebelum {formatTime(booking.expiresAt.slice(11, 19))}</span>
                </p>
                <Link to={`/bookings/${booking.bookingId}`} className="button">
                  Lanjutkan pembayaran
                </Link>
              </footer>
            </article>
          )
        })}
        {ordered.map(({ schedule, tickets: seats, minutesLeft }) => {
          const canCancel = minutesLeft > CANCEL_DEADLINE_MINUTES
          const startsIn = formatStartsIn(minutesLeft)
          return (
            <article
              key={schedule.scheduleId}
              className={['ticket', minutesLeft <= 0 && 'past', justBooked(schedule) && 'just-booked'].filter(Boolean).join(' ')}
              ref={justBooked(schedule) ? scrollOnce : undefined}
            >
              <header>
                <h2>
                  <Link to={`/films/${schedule.film.filmId}`}>{schedule.film.filmName}</Link>
                </h2>
                {minutesLeft <= 0 && <span className="badge off">Sudah tayang</span>}
                {startsIn && <span className="badge">{startsIn}</span>}
                <p className="muted">
                  {formatDate(schedule.filmDate)} · {formatTime(schedule.filmStartTime)}–{formatTime(schedule.filmEndTime)} ·
                  Studio {schedule.studioName}
                </p>
              </header>
              <ul className="ticket-seats" aria-label="Kursi">
                {seats.map((ticket) => (
                  <li key={ticket.ticketId} className={cancelling.includes(ticket.ticketId) ? 'busy' : undefined}>
                    <span className="seat-code">{ticket.seatsCode}</span>
                    <span className="muted" title="Nomor tiket">
                      #{ticket.ticketId}
                    </span>
                    {canCancel && (
                      <button
                        type="button"
                        className="icon"
                        aria-label={`Batalkan kursi ${ticket.seatsCode}`}
                        title={`Batalkan kursi ${ticket.seatsCode}`}
                        disabled={cancelling.length > 0}
                        onClick={() => cancel([ticket])}
                      >
                        <svg viewBox="0 0 16 16" width="14" height="14" aria-hidden="true">
                          <path d="M4 4l8 8M12 4l-8 8" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" />
                        </svg>
                      </button>
                    )}
                  </li>
                ))}
              </ul>
              <footer>
                {/* ponytail: tickets don't store their price, so this is the schedule's current price;
                    add a price column to tickets if admins start changing prices after sales */}
                <p className="ticket-total">
                  <strong>{formatRupiah(seats.length * schedule.ticketPrice)}</strong>{' '}
                  <span className="muted">
                    {seats.length} × {formatRupiah(schedule.ticketPrice)}
                  </span>
                </p>
                {canCancel && seats.length > 1 && (
                  <button type="button" className="secondary" disabled={cancelling.length > 0} onClick={() => cancel(seats)}>
                    {cancelling.length > 1 ? 'Membatalkan…' : `Batalkan semua (${seats.length} kursi)`}
                  </button>
                )}
                {minutesLeft > 0 && !canCancel && (
                  <p className="muted">Pembatalan ditutup 2 jam sebelum tayang.</p>
                )}
              </footer>
            </article>
          )
        })}
      </div>
    </section>
  )
}

function BookedNotice({ count, show }: { count: number; show?: Schedule }) {
  return (
    <p className="notice success" role="status">
      {show
        ? `Berhasil memesan ${count} kursi untuk ${show.film.filmName}, ${formatDate(show.filmDate)} pukul ${formatTime(show.filmStartTime)}.`
        : `Berhasil memesan ${count} kursi.`}
    </p>
  )
}
