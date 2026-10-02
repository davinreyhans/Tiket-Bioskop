import { useState } from 'react'
import { Link, useLocation } from 'react-router'
import { api, useApi, type Schedule, type Ticket } from '../api'
import { formatDate, formatTime, minutesUntilShow } from '../format'
import { ErrorMessage } from '../ui'

const CANCEL_DEADLINE_MINUTES = 120 // same rule as the backend: up to 2 hours before the show

export default function MyTicketsPage() {
  const location = useLocation()
  const tickets = useApi<Ticket[]>('/tickets/me')
  const [error, setError] = useState<string | null>(null)
  const [cancelling, setCancelling] = useState<number | null>(null)
  const booked = (location.state as { booked?: number } | null)?.booked

  async function cancel(ticket: Ticket) {
    if (!window.confirm(`Batalkan tiket kursi ${ticket.seatsCode}?`)) {
      return
    }
    setCancelling(ticket.ticketId)
    setError(null)
    try {
      await api(`/tickets/${ticket.ticketId}`, { method: 'DELETE' })
      tickets.reload()
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setCancelling(null)
    }
  }

  // one card per show, upcoming first
  const shows = new Map<number, { schedule: Schedule; tickets: Ticket[] }>()
  tickets.data?.forEach((ticket) => {
    const show = shows.get(ticket.schedule.scheduleId) ?? { schedule: ticket.schedule, tickets: [] }
    show.tickets.push(ticket)
    shows.set(ticket.schedule.scheduleId, show)
  })
  const ordered = [...shows.values()].sort((a, b) =>
    `${a.schedule.filmDate}T${a.schedule.filmStartTime}`.localeCompare(`${b.schedule.filmDate}T${b.schedule.filmStartTime}`),
  )

  return (
    <section>
      <h1>Tiket saya</h1>
      {booked && <p className="notice">Berhasil memesan {booked} kursi.</p>}
      <ErrorMessage error={error ?? tickets.error} />
      {tickets.loading && !tickets.data && <p className="muted">Memuat…</p>}
      {tickets.data?.length === 0 && (
        <p className="muted">
          Belum ada tiket. <Link to="/">Lihat film yang sedang tayang</Link>
        </p>
      )}

      <div className="stack">
        {ordered.map(({ schedule, tickets: seats }) => {
          const minutesLeft = minutesUntilShow(schedule.filmDate, schedule.filmStartTime)
          const canCancel = minutesLeft > CANCEL_DEADLINE_MINUTES
          return (
            <article key={schedule.scheduleId} className={minutesLeft <= 0 ? 'ticket past' : 'ticket'}>
              <header>
                <Link to={`/films/${schedule.film.filmId}`}>
                  <strong>{schedule.film.filmName}</strong>
                </Link>
                <span className="muted">
                  {formatDate(schedule.filmDate)} · {formatTime(schedule.filmStartTime)}–{formatTime(schedule.filmEndTime)} ·
                  Studio {schedule.studioName}
                </span>
                {minutesLeft <= 0 && <span className="badge off">Sudah tayang</span>}
              </header>
              <ul className="ticket-seats">
                {seats.map((ticket) => (
                  <li key={ticket.ticketId}>
                    <span className="seat-code">{ticket.seatsCode}</span>
                    {minutesLeft > 0 && (
                      <button
                        type="button"
                        className="secondary"
                        disabled={!canCancel || cancelling !== null}
                        onClick={() => cancel(ticket)}
                      >
                        {cancelling === ticket.ticketId ? 'Membatalkan…' : 'Batalkan'}
                      </button>
                    )}
                  </li>
                ))}
              </ul>
              {minutesLeft > 0 && !canCancel && (
                <p className="muted">Pembatalan ditutup 2 jam sebelum tayang.</p>
              )}
            </article>
          )
        })}
      </div>
    </section>
  )
}
