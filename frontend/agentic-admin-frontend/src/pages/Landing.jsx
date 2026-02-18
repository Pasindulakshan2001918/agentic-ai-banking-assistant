import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { Sun, Moon } from "lucide-react";
import { motion } from "framer-motion";

export default function Landing() {
  const [dark, setDark] = useState(true);

  useEffect(() => {
    if (dark) {
      document.documentElement.classList.add("dark");
    } else {
      document.documentElement.classList.remove("dark");
    }
  }, [dark]);

  return (
    <div
      className={`min-h-screen relative overflow-hidden transition-colors duration-500 ${
        dark
          ? "bg-gradient-to-br from-slate-950 via-slate-900 to-indigo-950 text-white"
          : "bg-gradient-to-br from-slate-100 via-white to-indigo-100 text-slate-900"
      }`}
    >
      {/* Subtle AI Network Background */}
      <div className="absolute inset-0 -z-10 opacity-20">
        <svg className="w-full h-full">
          {[...Array(20)].map((_, i) => (
            <motion.circle
              key={i}
              cx={`${Math.random() * 100}%`}
              cy={`${Math.random() * 100}%`}
              r="2"
              fill="#6366f1"
              animate={{ y: [0, -15, 0] }}
              transition={{
                duration: 8 + Math.random() * 5,
                repeat: Infinity,
              }}
            />
          ))}
        </svg>
      </div>

      {/* Glass Enterprise Navbar */}
      <header
        className={`fixed top-0 w-full z-50 backdrop-blur-xl border-b ${
          dark
            ? "bg-slate-900/40 border-slate-800"
            : "bg-white/40 border-slate-300"
        }`}
      >
        <div className="max-w-7xl mx-auto flex justify-between items-center px-10 py-5">
          <h1 className="text-2xl font-bold tracking-wide">
            Agentic<span className="text-indigo-500">Bank</span>
          </h1>

          <div className="flex items-center gap-6">
            <button
              onClick={() => setDark(!dark)}
              className="p-2 rounded-full hover:bg-indigo-500/20 transition"
            >
              {dark ? <Sun size={20} /> : <Moon size={20} />}
            </button>

            <Link
              to="/login"
              className="px-6 py-2 rounded-xl bg-indigo-600 text-white shadow-lg hover:bg-indigo-700 transition font-semibold"
            >
              Login
            </Link>
          </div>
        </div>
      </header>

      {/* Split Enterprise Layout */}
      <main className="pt-32 max-w-7xl mx-auto px-10 grid md:grid-cols-2 items-center min-h-screen gap-12">
        
        {/* LEFT — Corporate Messaging */}
        <div>
          <motion.h2
            initial={{ opacity: 0, y: 40 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8 }}
            className="text-5xl font-extrabold leading-tight mb-6"
          >
            Administrative Control Panel for
            <span className="text-indigo-500"> Digital Banking</span>
          </motion.h2>

          <p
            className={`text-lg mb-6 ${
              dark ? "text-slate-300" : "text-slate-700"
            }`}
          >
            Secure access to banking operations management, transaction monitoring,
            user administration, and compliance tools within a regulated
            financial environment.
          </p>

          <div
            className={`mb-6 text-xs font-semibold tracking-wider ${
              dark ? "text-red-400" : "text-red-600"
            }`}
          >
            AUTHORIZED USERS ONLY
          </div>

          <p
            className={`text-sm mb-8 ${
              dark ? "text-slate-400" : "text-slate-600"
            }`}
          >
            This system is restricted to authorized banking personnel.
            Unauthorized access attempts will be prosecuted to the fullest
            extent of the law.
          </p>

          <div className="flex gap-6">
            <Link
              to="/login"
              className="px-8 py-3 rounded-2xl bg-indigo-600 text-white shadow-xl hover:bg-indigo-700 transition font-bold"
            >
              Secure Login
            </Link>

            <button
              className={`px-8 py-3 rounded-2xl border font-bold transition ${
                dark
                  ? "border-indigo-400 text-indigo-300 hover:bg-indigo-500/20"
                  : "border-indigo-600 text-indigo-700 hover:bg-indigo-100"
              }`}
            >
              System Overview
            </button>
          </div>
        </div>

        {/* RIGHT — Premium Hero Card */}
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.8 }}
          className="relative"
        >
          <div className="absolute inset-0 bg-indigo-600 blur-3xl opacity-20 animate-pulse rounded-3xl"></div>

          <div
            className={`relative rounded-3xl p-12 shadow-2xl backdrop-blur-xl border ${
              dark
                ? "bg-slate-900/70 border-slate-800"
                : "bg-white/70 border-slate-300"
            }`}
          >
            <h3 className="text-2xl font-bold mb-6">
              Security & Compliance Notice
            </h3>

            <div className="space-y-4 text-sm leading-relaxed">
              <div>
                This is a restricted administrative system for authorized
                banking personnel only.
              </div>

              <div>
                All activities are continuously monitored, recorded, and logged
                in accordance with applicable financial regulations.
              </div>

              <div>
                Access to this platform constitutes consent to monitoring,
                auditing, and compliance verification procedures.
              </div>

              <div className="pt-4 border-t border-slate-700/40 text-xs text-slate-400">
                Secure System: Licensed by Central Bank of Sri Lanka •
                All activities monitored and logged
              </div>
            </div>
          </div>
        </motion.div>
      </main>

      <footer
        className={`text-center py-6 text-sm ${
          dark ? "text-slate-500" : "text-slate-600"
        }`}
      >
        © 2026 Agentic Bank • Confidential Internal Administrative System
      </footer>
    </div>
  );
}
