const rupiah = new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 })

export function formatRupiah(value: number) {
  return rupiah.format(value)
}

// "2026-10-01" -> "Kamis, 1 Oktober 2026"; built from parts so the browser's time zone can't shift the day
export function formatDate(isoDate: string) {
  const [year, month, day] = isoDate.split('-').map(Number)
  return new Date(year, month - 1, day).toLocaleDateString('id-ID', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  })
}

// "19:00:00" -> "19:00"
export function formatTime(time: string) {
  return time.slice(0, 5)
}

// Same zone as `bioskop.timezone` in the backend: schedules are local cinema time
const CINEMA_TIME_ZONE = 'Asia/Jakarta'

// "2026-10-08T19:30:00" in cinema time, whatever the browser's time zone
function cinemaNow() {
  return new Date().toLocaleString('sv-SE', { timeZone: CINEMA_TIME_ZONE }).replace(' ', 'T')
}

// "2026-10-08": today in cinema time, for the backend's ?from= filter
export function cinemaToday() {
  return cinemaNow().slice(0, 10)
}

// Minutes from now until the show starts (negative = already started), measured in cinema time.
// Both wall-clock times are read as if they were UTC, so the browser's own time zone never matters.
export function minutesUntilShow(filmDate: string, startTime: string) {
  return (Date.parse(`${filmDate}T${startTime}Z`) - Date.parse(`${cinemaNow()}Z`)) / 60_000
}

// "Hari ini", "Besok" or "Sab, 10 Okt": short day labels for showtimes
export function formatDay(isoDate: string) {
  const days = (Date.parse(`${isoDate}T00:00:00Z`) - Date.parse(`${cinemaToday()}T00:00:00Z`)) / 86_400_000
  if (days === 0) return 'Hari ini'
  if (days === 1) return 'Besok'
  const [year, month, day] = isoDate.split('-').map(Number)
  return new Date(year, month - 1, day).toLocaleDateString('id-ID', { weekday: 'short', day: 'numeric', month: 'short' })
}
