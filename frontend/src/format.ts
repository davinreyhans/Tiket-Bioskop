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

// Minutes from now until the show starts (negative = already started), measured in cinema time.
// Both wall-clock times are read as if they were UTC, so the browser's own time zone never matters.
export function minutesUntilShow(filmDate: string, startTime: string) {
  const now = new Date().toLocaleString('sv-SE', { timeZone: CINEMA_TIME_ZONE }).replace(' ', 'T')
  return (Date.parse(`${filmDate}T${startTime}Z`) - Date.parse(`${now}Z`)) / 60_000
}
