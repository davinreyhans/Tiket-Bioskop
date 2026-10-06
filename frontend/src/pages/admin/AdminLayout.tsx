import { NavLink, Outlet } from 'react-router'

export default function AdminLayout() {
  return (
    <section>
      <h1>Admin</h1>
      <nav className="tabs" aria-label="Menu admin">
        <NavLink to="/admin/films">Film</NavLink>
        <NavLink to="/admin/schedules">Jadwal</NavLink>
        <NavLink to="/admin/users">User</NavLink>
      </nav>
      <Outlet />
    </section>
  )
}
