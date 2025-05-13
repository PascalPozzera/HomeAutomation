import { useState, useEffect } from "react";

export default function MediaStationComponent() {
    const [open, setOpen] = useState(false);
    const [isPlaying, setIsPlaying] = useState(false);
    const [currentMovie, setCurrentMovie] = useState<string | null>(null);

    // Poll Media status
    useEffect(() => {
        const interval = setInterval(fetchStatus, 2000);
        return () => clearInterval(interval);
    }, []);

    const fetchStatus = async () => {
        try {
            const res = await fetch("/api/media/status");
            const data = await res.json();
            setIsPlaying(data.playing);
            setCurrentMovie(data.playing ? data.title : null);
        } catch (err) {
            console.error("Failed to fetch media status", err);
        }
    };

    const playMovie = async () => {
        try {
            await fetch("/api/media/play?title=Inception", { method: "POST" });
            await fetchStatus();
        } catch (err) {
            console.error("Failed to play movie", err);
        }
    };

    const stopMovie = async () => {
        try {
            await fetch("/api/media/stop", { method: "POST" });
            await fetchStatus();
        } catch (err) {
            console.error("Failed to stop movie", err);
        }
    };

    return (
        <>
            {/* Box */}
            <div
                onClick={() => setOpen(true)}
                className="relative bg-white shadow-md rounded-xl p-4 text-center cursor-pointer hover:shadow-lg transition"
            >
                {/* LED */}
                <div className="absolute top-3 right-3">
                    <div
                        className={`w-3 h-3 rounded-full ${
                            isPlaying ? "bg-green-500" : "bg-red-500"
                        } border border-gray-300`}
                        title={isPlaying ? "Playing" : "Stopped"}
                    />
                </div>

                <img src="/mediastation-icon.jpg" alt="Media" className="w-16 h-16 mx-auto mb-2" />
                <h2 className="text-lg font-semibold text-gray-800">Media Station</h2>
                <p className="text-sm text-gray-500">
                    {isPlaying ? `Now playing: ${currentMovie}` : "No movie playing"}
                </p>
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
                        <div className="flex justify-between items-center mb-6">
                            <h3 className="text-xl font-bold text-gray-800">Media Control</h3>
                            <button
                                onClick={() => setOpen(false)}
                                className="text-gray-500 hover:text-black text-2xl"
                            >
                                &times;
                            </button>
                        </div>

                        <div className="text-center text-gray-700 mb-4">
                            {isPlaying ? (
                                <p className="text-lg">🎬 Playing: <strong>{currentMovie}</strong></p>
                            ) : (
                                <p className="text-lg text-gray-500">No movie is playing</p>
                            )}
                        </div>

                        <div className="space-y-2">
                            <button
                                onClick={playMovie}
                                className="w-full bg-indigo-500 text-white py-2 rounded-lg hover:bg-indigo-600"
                            >
                                ▶️ Play "Inception"
                            </button>
                            <button
                                onClick={stopMovie}
                                className="w-full bg-gray-200 text-black py-2 rounded-lg hover:bg-gray-300"
                            >
                                ⏹️ Stop Movie
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </>
    );
}
