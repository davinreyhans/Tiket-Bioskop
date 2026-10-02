import type { ReactNode } from 'react'
import { Link, Navigate, NavLink, Route, Routes, useLocation } from 'react-router'
import { clearSession, useUser } from './auth'
import AdminFilmsPage from './pages/admin/AdminFilmsPage'
import AdminLayout from './pages/admin/AdminLayout'
import AdminSchedulesPage from './pages/admin/AdminSchedulesPage'
import AdminUsersPage from './pages/admin/AdminUsersPage'
import FilmDetailPage from './pages/FilmDetailPage'
import FilmsPage from './pages/FilmsPage'
import LoginPage from './pages/LoginPage'
import MyTicketsPage from './pages/MyTicketsPage'
import ProfilePage from './pages/ProfilePage'
import RegisterPage from './pages/RegisterPage'
import SeatsPage from './pages/SeatsPage'

// Logged-out visitors go to the login page and come back here afterwards.
// The backend enforces the same rules; this only keeps people off pages that would fail.
function RequireLogin({ admin = false, children }: { admin?: boolean; children: ReactNode }) {
  const user = useUser()
  const location = useLocation()
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  if (admin && user.role !== 'ADMIN') {
    return (
      <section>
        <h1>Khusus admin</h1>
        <p className="muted">Akun kamu tidak punya akses ke halaman ini.</p>
      </section>
    )
  }
  return children
}

export default function App() {
  const user = useUser()

  return (
    <>
      <header className="topbar">
        <Link to="/" className="brand">
          Tiket Bioskop
        </Link>
        <nav>
          <NavLink to="/" end>
            Film
          </NavLink>
          {user ? (
            <>
              {user.role === 'ADMIN' && <NavLink to="/admin">Admin</NavLink>}
              <NavLink to="/tickets">Tiket saya</NavLink>
              <NavLink to="/profile">{user.username}</NavLink>
              <button type="button" className="secondary" onClick={clearSession}>
                Keluar
              </button>
            </>
          ) : (
            <>
              <NavLink to="/login">Masuk</NavLink>
              <NavLink to="/register">Daftar</NavLink>
            </>
          )}
        </nav>
      </header>

      <main>
        <Routes>
          <Route path="/" element={<FilmsPage />} />
          <Route path="/films/:filmId" element={<FilmDetailPage />} />
          <Route path="/schedules/:scheduleId" element={<SeatsPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route
            path="/tickets"
            element={
              <RequireLogin>
                <MyTicketsPage />
              </RequireLogin>
            }
          />
          <Route
            path="/profile"
            element={
              <RequireLogin>
                <ProfilePage />
              </RequireLogin>
            }
          />
          <Route
            path="/admin"
            element={
              <RequireLogin admin>
                <AdminLayout />
              </RequireLogin>
            }
          >
            <Route index element={<Navigate to="films" replace />} />
            <Route path="films" element={<AdminFilmsPage />} />
            <Route path="schedules" element={<AdminSchedulesPage />} />
            <Route path="users" element={<AdminUsersPage />} />
          </Route>
          <Route
            path="*"
            element={
              <section>
                <h1>Halaman tidak ditemukan</h1>
                <Link to="/">‹ Kembali ke daftar film</Link>
              </section>
            }
          />
        </Routes>
      </main>
    </>
  )
}
