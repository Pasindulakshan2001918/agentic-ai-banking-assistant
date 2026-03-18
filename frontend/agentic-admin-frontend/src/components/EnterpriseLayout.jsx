import { useState, useEffect } from "react";
import { Sun, Moon } from "lucide-react";
import keycloak from "../keycloak";

export default function EnterpriseLayout({ title, children }) {
  const [dark, setDark] = useState(true);

  useEffect(() => {
    if (dark) {
      document.documentElement.classList.add("dark");
    } else {
      document.documentElement.classList.remove("dark");
    }
  }, [dark]);

  return (
    <div className={`min-h-screen transition-colors duration-500 ${
      dark
        ? "bg-gradient-to-br from-slate-950 via-slate-900 to-indigo-950 text-white"
        : "bg-gradient-to-br from-slate-100 via-white to-indigo-100 text-slate-900"
    }`}>

      {/* Navbar */}
      <header className="fixed top-0 w-full backdrop-blur-xl border-b bg-slate-900/40 border-slate-800 z-50">
        <div className="max-w-7xl mx-auto flex justify-between items-center px-10 py-5">
          <h1 className="text-2xl font-bold">
            Agentic<span className="text-indigo-500">Bank</span>
          </h1>

          <div className="flex items-center gap-6">
            <button onClick={() => setDark(!dark)}>
              {dark ? <Sun size={20}/> : <Moon size={20}/>}
            </button>

            <button
              onClick={() =>
                keycloak.logout({ redirectUri: window.location.origin })
              }
              className="px-4 py-2 bg-indigo-600 rounded-xl hover:bg-indigo-700"
            >
              Logout
            </button>
          </div>
        </div>
      </header>

      <main className="pt-32 max-w-7xl mx-auto px-10">
        <h2 className="text-4xl font-bold mb-10">{title}</h2>
        {children}
      </main>
    </div>
  );
}