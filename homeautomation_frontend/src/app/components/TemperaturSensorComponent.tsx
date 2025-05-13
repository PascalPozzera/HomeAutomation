import { useState } from "react";

export default function TemperatureSensorComponent() {
    const [open, setOpen] = useState(false);
    const [mode, setMode] = useState<"SIMULATION" | "MANUAL" | null>(null);
    const [environmentSource, setEnvironmentSource] = useState<"INTERNAL" | "EXTERNAL" | "MANUAL" | null>(null);

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
            {/* Sensor Box */}
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

            {/* Modal */}
            {open && (
                <div
                    className="fixed inset-0 backdrop-blur-sm bg-white/20 flex items-center justify-center z-50"
                    onClick={() => setOpen(false)}
                >
                    <div
                        className="bg-white rounded-xl p-6 w-96 shadow-2xl"
                        onClick={(e) => e.stopPropagation()}
                    >
                        {/* Header */}
                        <div className="flex justify-between items-center mb-6">
                            <h3 className="text-xl font-bold text-gray-800">Sensor Settings</h3>
                            <button
                                onClick={() => setOpen(false)}
                                className="text-gray-500 hover:text-black text-2xl"
                            >
                                &times;
                            </button>
                        </div>

                        {/* Environment Source */}
                        <div className="space-y-2 mb-6">
                            <p className="text-sm font-medium text-gray-600">🌍 Environment Source</p>
                            <div className="space-y-2">
                                <button
                                    onClick={() => switchEnvironmentSource("INTERNAL")}
                                    className="w-full bg-blue-50 text-blue-800 py-2 rounded-lg hover:bg-blue-100"
                                >
                                    Internal
                                </button>
                                <button
                                    onClick={() => switchEnvironmentSource("EXTERNAL")}
                                    className="w-full bg-purple-50 text-purple-800 py-2 rounded-lg hover:bg-purple-100"
                                >
                                    External
                                </button>
                                <button
                                    onClick={() => switchEnvironmentSource("MANUAL")}
                                    className="w-full bg-gray-100 text-gray-800 py-2 rounded-lg hover:bg-gray-200"
                                >
                                    Manual
                                </button>
                            </div>
                        </div>

                        {/* Temperature Simulation */}
                        {environmentSource === "MANUAL" && (
                            <div className="space-y-2 mb-6">
                                <p className="text-sm font-medium text-gray-600">🌡️ Simulate Temperature</p>
                                <div className="flex gap-3">
                                    <button
                                        onClick={() => simulateTemp(30)}
                                        className="flex-1 bg-white text-gray-700 border border-gray-300 py-2 rounded-lg hover:bg-gray-50"
                                    >
                                        30°C
                                    </button>
                                    <button
                                        onClick={() => simulateTemp(8)}
                                        className="flex-1 bg-white text-gray-700 border border-gray-300 py-2 rounded-lg hover:bg-gray-50"
                                    >
                                        8°C
                                    </button>
                                </div>
                            </div>
                        )}

                        {/* Weather Control */}
                        {environmentSource === "MANUAL" && (
                            <div className="space-y-2">
                                <p className="text-sm font-medium text-gray-600">⛅ Set Weather</p>
                                <div className="grid grid-cols-2 gap-2">
                                    <button
                                        onClick={() => setWeather("SUNNY")}
                                        className="bg-yellow-100 text-yellow-900 py-2 rounded-lg hover:bg-yellow-200"
                                    >
                                        ☀️ Sunny
                                    </button>
                                    <button
                                        onClick={() => setWeather("CLOUDY")}
                                        className="bg-gray-300 text-gray-800 py-2 rounded-lg hover:bg-gray-400"
                                    >
                                        ☁️ Cloudy
                                    </button>
                                    <button
                                        onClick={() => setWeather("RAINY")}
                                        className="bg-blue-300 text-blue-900 py-2 rounded-lg hover:bg-blue-400"
                                    >
                                        🌧️ Rainy
                                    </button>
                                    <button
                                        onClick={() => setWeather("SNOWY")}
                                        className="bg-white text-gray-700 py-2 rounded-lg border hover:bg-gray-50"
                                    >
                                        ❄️ Snowy
                                    </button>
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            )}
        </>
    );
}
