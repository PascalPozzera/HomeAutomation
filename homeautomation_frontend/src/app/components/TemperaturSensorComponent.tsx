import { useState, useRef, useEffect } from "react";

export default function TemperatureSensorComponent() {
    const [open, setOpen] = useState(false);
    const [mode, setMode] = useState<"SIMULATION" | "MANUAL" | null>(null);
    const [environmentSource, setEnvironmentSource] = useState<"INTERNAL" | "EXTERNAL" | "MANUAL" | null>(null);
    const modalRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (modalRef.current && !modalRef.current.contains(event.target as Node)) {
                setOpen(false);
            }
        };

        if (open) {
            document.addEventListener("mousedown", handleClickOutside);
        }

        return () => {
            document.removeEventListener("mousedown", handleClickOutside);
        };
    }, [open]);

    const switchMode = async (simulate: boolean) => {
        await fetch(`/api/ac/sensor-mode?simulate=${simulate}`, { method: "POST" });
        setMode(simulate ? "SIMULATION" : "MANUAL");
    };

    const simulateTemp = async (value: number) => {
        await fetch(`/api/environment/temperature?value=${value}`, { method: "POST" });
    };

    const switchEnvironmentSource = async (type: "INTERNAL" | "EXTERNAL" | "MANUAL") => {
        await fetch(`/api/environment/source?type=${type}`, { method: "POST" });
        setEnvironmentSource(type);
    };

    const setWeather = async (condition: "SUNNY" | "CLOUDY" | "RAINY" | "SNOWY") => {
        await fetch(`/api/environment/weather?condition=${condition}`, { method: "POST" });
    };

    return (
        <>
            <div
                onClick={() => setOpen(true)}
                className="relative bg-white shadow-md rounded-xl p-4 text-center cursor-pointer hover:shadow-lg transition"
            >
                <div className="absolute top-3 right-3">
                    {mode !== null && (
                        <div
                            className={`w-3 h-3 rounded-full ${
                                mode === "SIMULATION" ? "bg-green-500" : "bg-yellow-500"
                            } border border-gray-300`}
                            title={`Mode: ${mode}`}
                        />
                    )}
                </div>

                <img src="/temperature-icon.png" alt="Temperature" className="w-16 h-16 mx-auto mb-2" />
                <h2 className="text-lg font-semibold text-gray-800">Temperature Sensor</h2>
                <p className="text-sm text-gray-500">Manage temperature and weather</p>
            </div>

            {open && (
                <div className="fixed inset-0 bg-white/30 backdrop-blur-sm flex items-center justify-center z-50">
                    <div
                        ref={modalRef}
                        className="bg-white rounded-xl p-6 w-96 shadow-2xl"
                    >
                        <div className="flex justify-between items-center mb-6">
                            <h3 className="text-xl font-bold text-gray-800">Sensor Settings</h3>
                            <button
                                onClick={() => setOpen(false)}
                                className="text-gray-500 hover:text-black text-2xl"
                            >
                                &times;
                            </button>
                        </div>

                        <div className="space-y-2 mb-6">
                            <p className="text-sm font-medium text-gray-600">🌍 Environment Source</p>
                            <div className="space-y-2">
                                {["INTERNAL", "EXTERNAL", "MANUAL"].map((type) => (
                                    <button
                                        key={type}
                                        onClick={() => switchEnvironmentSource(type as any)}
                                        className={`w-full py-2 rounded-lg ${
                                            environmentSource === type
                                                ? "bg-blue-600 text-white"
                                                : "bg-gray-100 text-gray-800 hover:bg-gray-200"
                                        }`}
                                    >
                                        {type.charAt(0) + type.slice(1).toLowerCase()}
                                    </button>
                                ))}
                            </div>
                        </div>

                        {environmentSource === "MANUAL" && (
                            <div className="space-y-2 mb-6">
                                <p className="text-sm font-medium text-gray-600">🌡️ Simulate Temperature</p>
                                <div className="flex gap-3">
                                    {[30, 8].map((value) => (
                                        <button
                                            key={value}
                                            onClick={() => simulateTemp(value)}
                                            className="flex-1 bg-white text-gray-700 border border-gray-300 py-2 rounded-lg hover:bg-gray-50"
                                        >
                                            {value}°C
                                        </button>
                                    ))}
                                </div>
                            </div>
                        )}

                        {environmentSource === "MANUAL" && (
                            <div className="space-y-2">
                                <p className="text-sm font-medium text-gray-600">⛅ Set Weather</p>
                                <div className="grid grid-cols-2 gap-2">
                                    {[
                                        { label: "☀️ Sunny", value: "SUNNY", bg: "bg-yellow-100", text: "text-yellow-900" },
                                        { label: "☁️ Cloudy", value: "CLOUDY", bg: "bg-gray-300", text: "text-gray-800" },
                                        { label: "🌧️ Rainy", value: "RAINY", bg: "bg-blue-300", text: "text-blue-900" },
                                        { label: "❄️ Snowy", value: "SNOWY", bg: "bg-white", text: "text-gray-700 border" },
                                    ].map(({ label, value, bg, text }) => (
                                        <button
                                            key={value}
                                            onClick={() => setWeather(value as any)}
                                            className={`${bg} ${text} py-2 rounded-lg hover:opacity-80`}
                                        >
                                            {label}
                                        </button>
                                    ))}
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            )}
        </>
    );
}
