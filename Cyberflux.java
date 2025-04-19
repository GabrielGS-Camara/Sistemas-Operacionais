import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class CyberCafe {
    private static final int PCS = 10;
    private static final int HEADSETS = 6;
    private static final int CADEIRAS = 8;

    private final Semaphore pcSemaphore = new Semaphore(PCS);
    private final Semaphore headsetSemaphore = new Semaphore(HEADSETS);
    private final Semaphore cadeiraSemaphore = new Semaphore(CADEIRAS);
    
    private final AtomicInteger clientesAtendidos = new AtomicInteger(0);
    private final AtomicInteger clientesNaoAtendidos = new AtomicInteger(0);
    private final AtomicInteger usoPCs = new AtomicInteger(0);
    private final AtomicInteger usoHeadsets = new AtomicInteger(0);
    private final AtomicInteger usoCadeiras = new AtomicInteger(0);

    public int getClientesAtendidos() { return clientesAtendidos.get(); }
    public int getClientesNaoAtendidos() { return clientesNaoAtendidos.get(); }
    public int getUsoPCs() { return usoPCs.get(); }
    public int getUsoHeadsets() { return usoHeadsets.get(); }
    public int getUsoCadeiras() { return usoCadeiras.get(); }

    public void usarRecursos(String tipoCliente, boolean precisaPC, boolean precisaHeadset, boolean precisaCadeira) {
        boolean conseguiuPC = false, conseguiuHeadset = false, conseguiuCadeira = false;

        try {
            if (precisaPC) conseguiuPC = pcSemaphore.tryAcquire();
            if (precisaHeadset) conseguiuHeadset = headsetSemaphore.tryAcquire();
            if (precisaCadeira) conseguiuCadeira = cadeiraSemaphore.tryAcquire();

            if (precisaPC && !conseguiuPC || precisaHeadset && !conseguiuHeadset || precisaCadeira && !conseguiuCadeira) {
                System.out.print(tipoCliente + " não foi atendido. Motivo: ");
                if (!conseguiuPC) System.out.print("PCs esgotados. ");
                if (!conseguiuHeadset) System.out.print("Headsets esgotados. ");
                if (!conseguiuCadeira) System.out.print("Cadeiras esgotadas. ");
                System.out.println();
                clientesNaoAtendidos.incrementAndGet();
            } else {
                clientesAtendidos.incrementAndGet();
                if (precisaPC) usoPCs.incrementAndGet();
                if (precisaHeadset) usoHeadsets.incrementAndGet();
                if (precisaCadeira) usoCadeiras.incrementAndGet();

                int tempoUso = ThreadLocalRandom.current().nextInt(2000, 5000);
                System.out.println(tipoCliente + " está usando os recursos por " + tempoUso / 1000 + " segundos.");
                Thread.sleep(tempoUso);

                System.out.println(tipoCliente + " liberou os equipamentos.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (conseguiuPC) pcSemaphore.release();
            if (conseguiuHeadset) headsetSemaphore.release();
            if (conseguiuCadeira) cadeiraSemaphore.release();
        }
    }
}

class Cliente implements Runnable {
    private final CyberCafe cafe;
    private final String tipo;
    private final boolean precisaPC;
    private final boolean precisaHeadset;
    private final boolean precisaCadeira;

    public Cliente(CyberCafe cafe, String tipo, boolean precisaPC, boolean precisaHeadset, boolean precisaCadeira) {
        this.cafe = cafe;
        this.tipo = tipo;
        this.precisaPC = precisaPC;
        this.precisaHeadset = precisaHeadset;
        this.precisaCadeira = precisaCadeira;
    }

    @Override
    public void run() {
        cafe.usarRecursos(tipo, precisaPC, precisaHeadset, precisaCadeira);
    }
}

public class Cyberflux {
    public static void main(String[] args) {
        CyberCafe cafe = new CyberCafe();
        ExecutorService executor = Executors.newCachedThreadPool();
        long startTime = System.currentTimeMillis();
        long duration = 48000;

        while (System.currentTimeMillis() - startTime < duration) {
            for (int i = 0; i < 3; i++) {
                int tipoCliente = ThreadLocalRandom.current().nextInt(3);
                switch (tipoCliente) {
                    case 0: executor.execute(new Cliente(cafe, "Gamer", true, true, false)); break;
                    case 1: executor.execute(new Cliente(cafe, "Freelancer", true, false, true)); break;
                    case 2: executor.execute(new Cliente(cafe, "Estudante", true, false, false)); break;
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("\n--- Relatório Final ---");
        System.out.println("Clientes Atendidos: " + cafe.getClientesAtendidos());
        System.out.println("Clientes Não Atendidos: " + cafe.getClientesNaoAtendidos());
        System.out.println("Uso de PCs: " + cafe.getUsoPCs());
        System.out.println("Uso de Headsets: " + cafe.getUsoHeadsets());
        System.out.println("Uso de Cadeiras: " + cafe.getUsoCadeiras());
    }
}
