package com.example.demo.web.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.demo.web.domain.Admin;
import com.example.demo.web.domain.Bairro;
import com.example.demo.web.domain.Distrito;
import com.example.demo.web.domain.Provincia;
import com.example.demo.web.repository.AdminRepository;
import com.example.demo.web.repository.BairroRepository;
import com.example.demo.web.repository.DistritoRepository;
import com.example.demo.web.repository.ProvinciaRepository;

/**
 * Popula automaticamente a base de dados com os dados geográficos
 * de Moçambique (Províncias → Distritos → Bairros) na primeira execução.
 * Também cria o admin padrão se ainda não existir.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final ProvinciaRepository provinciaRepo;
    private final DistritoRepository distritoRepo;
    private final BairroRepository bairroRepo;
    private final AdminRepository adminRepo;

    public DataInitializer(ProvinciaRepository provinciaRepo,
                           DistritoRepository distritoRepo,
                           BairroRepository bairroRepo,
                           AdminRepository adminRepo) {
        this.provinciaRepo = provinciaRepo;
        this.distritoRepo  = distritoRepo;
        this.bairroRepo    = bairroRepo;
        this.adminRepo     = adminRepo;
    }

    @Override
    public void run(String... args) {

        // Criar admin padrão se não existir; se já existir, garantir superAdmin=true
        var adminOpt = adminRepo.findByEmail("admin@sgpa.mz");
        if (adminOpt.isEmpty()) {
            Admin admin = new Admin();
            admin.setNome("Super");
            admin.setApelido("Admin");
            admin.setEmail("admin@sgpa.mz");
            admin.setSenha("admin123");
            admin.setSuperAdmin(true);
            adminRepo.save(admin);
            System.out.println(">>> [DataInitializer] SuperAdmin padrão criado: admin@sgpa.mz / admin123");
        } else {
            Admin admin = adminOpt.get();
            if (!admin.isSuperAdmin()) {
                admin.setSuperAdmin(true);
                adminRepo.save(admin);
                System.out.println(">>> [DataInitializer] SuperAdmin flag activado para: admin@sgpa.mz");
            }
        }

        // Só inicializa se a tabela estiver vazia
        if (provinciaRepo.count() > 0) return;

        System.out.println(">>> [DataInitializer] A popular dados geográficos...");

       // =====================================================================
        // CIDADE DE MAPUTO
        // =====================================================================
        Provincia cidadeMaputo = salvarProvincia("Cidade de Maputo");

        Distrito kampfumo = salvarDistrito("KaMpfumo", cidadeMaputo);
        salvarBairros(kampfumo,
                "Alto Maé A","Alto Maé B","Ayala","Central","Malhangalene A",
                "Malhangalene B","Polana Cimento A","Polana Cimento B",
                "Sommerschield I","Sommerschield II");

        Distrito kamaxaquene = salvarDistrito("KaMaxaquene", cidadeMaputo);
        salvarBairros(kamaxaquene,
                "Chamanculo A","Chamanculo B","Chamanculo C","Chamanculo D",
                "Maxaquene A","Maxaquene B","Maxaquene C","Maxaquene D",
                "Polana Caniço A","Polana Caniço B");

        Distrito kamavota = salvarDistrito("KaMavota", cidadeMaputo);
        salvarBairros(kamavota,
                "Ferroviário","Mahotas","Albasine","FPLM","Hulene A","Hulene B","Laulane",
                "Magoanine A","Magoanine B","Magoanine C",
                "Mavalane A","Mavalane B","Costa Do Sol");

        Distrito kamubukwana = salvarDistrito("KaMubukwana", cidadeMaputo);
        salvarBairros(kamubukwana,
                "Bagamoyo","George Dimitrov","Inhagóia A","Inhagóia B",
                "Luís Cabral","Zimpeto");

        Distrito nlhamankulu = salvarDistrito("Nlhamankulu", cidadeMaputo);
        salvarBairros(nlhamankulu,
                "Munhuana","Xipamanine","Urbanização","Xiquelene");

        Distrito katembe = salvarDistrito("KaTembe", cidadeMaputo);
        salvarBairros(katembe, "Chali","Guachene","Inguide");

        Distrito kanyaka = salvarDistrito("KaNyaka", cidadeMaputo);
        salvarBairros(kanyaka, "Inguane","Ribjene");

        // =====================================================================
        // MAPUTO (PROVÍNCIA)
        // =====================================================================
        Provincia maputo = salvarProvincia("Maputo");

        Distrito boane = salvarDistrito("Boane", maputo);
        salvarBairros(boane, "Beluluane","Boane Sede","Mahubo","Matola Rio");

        Distrito magude = salvarDistrito("Magude", maputo);
        salvarBairros(magude, "Magude Sede","Mapulanguene","Motaze","Pinda");

        Distrito manhica = salvarDistrito("Manhiça", maputo);
        salvarBairros(manhica, "Ilha Josina Machel","Manhiça Sede","Maragra","Xinavane");

        Distrito marracuene = salvarDistrito("Marracuene", maputo);
        salvarBairros(marracuene, "Machubo","Marracuene Sede","Michafutene","Nhongonhane");

        Distrito matola = salvarDistrito("Matola", maputo);
        salvarBairros(matola,
                "Bairro Jardim","Fomento","Infulene","Liberdade",
                "Machava","Matola A","Matola B","Matola C","Matola D",
                "Matola Gare","Muhalaze","Ndlavela","Tsalala");

        Distrito matutuine = salvarDistrito("Matutuíne", maputo);
        salvarBairros(matutuine, "Bela Vista","Catuane","Matutuíne Sede","Salamanga");

        Distrito moamba = salvarDistrito("Moamba", maputo);
        salvarBairros(moamba, "Moamba Sede","Ressano Garcia","Sabie","Tenga");

        Distrito namaacha = salvarDistrito("Namaacha", maputo);
        salvarBairros(namaacha, "Changalane","Goba","Namaacha Sede");

        // =====================================================================
        // GAZA
        // =====================================================================
        Provincia gaza = salvarProvincia("Gaza");

        Distrito bilene = salvarDistrito("Bilene", gaza);
        salvarBairros(bilene, "Macia","Praia do Bilene","Siaia");

        Distrito chibuto = salvarDistrito("Chibuto", gaza);
        salvarBairros(chibuto, "Chibuto Sede","Malehice","Changanine");

        Distrito chicualacuala = salvarDistrito("Chicualacuala", gaza);
        salvarBairros(chicualacuala, "Chicualacuala Sede","Mapai","Vista Alegre");

        Distrito chigubo = salvarDistrito("Chigubo", gaza);
        salvarBairros(chigubo, "Chigubo Sede","Ngudu");

        Distrito chokwe = salvarDistrito("Chókwè", gaza);
        salvarBairros(chokwe, "Chókwè Sede","Lionde","Macarretane");

        Distrito chongoene = salvarDistrito("Chongoene", gaza);
        salvarBairros(chongoene, "Chongoene Sede","Quissico direction","Zongoene");

        Distrito guija = salvarDistrito("Guijá", gaza);
        salvarBairros(guija, "Eduardo Mondlane","Guijá Sede","Mabalane direction");

        Distrito limpopo = salvarDistrito("Limpopo", gaza);
        salvarBairros(limpopo, "Chokwé Area","Limpopo Sede");

        Distrito mabalane = salvarDistrito("Mabalane", gaza);
        salvarBairros(mabalane, "Mabalane Sede","Pafuri direction");

        Distrito mandlakazi = salvarDistrito("Mandlakazi", gaza);
        salvarBairros(mandlakazi, "Incoluane","Mandlakazi Sede","Tavane");

        Distrito massangena = salvarDistrito("Massangena", gaza);
        salvarBairros(massangena, "Massangena Sede");

        Distrito massingir = salvarDistrito("Massingir", gaza);
        salvarBairros(massingir, "Massingir Sede","Massingir Velho");

        Distrito xaixai = salvarDistrito("Xai-Xai", gaza);
        salvarBairros(xaixai,
                "Bairro 1","Bairro 2","Bairro 3","Banhine",
                "Patrice Lumumba","Xai-Xai Cidade","Zongoene");

        // =====================================================================
        // INHAMBANE
        // =====================================================================
        Provincia inhambane = salvarProvincia("Inhambane");

        Distrito cidadeInhambane = salvarDistrito("Inhambane Cidade", inhambane);
        salvarBairros(cidadeInhambane,
                "Balane","Chambone","Chalambe","Liberdade","Muelé","Murrebue","Salela");

        Distrito funhalouro = salvarDistrito("Funhalouro", inhambane);
        salvarBairros(funhalouro, "Funhalouro Sede","Mabote direction");

        Distrito govuro = salvarDistrito("Govuro", inhambane);
        salvarBairros(govuro, "Machanga","Nova Mambone","Save direction");

        Distrito homoine = salvarDistrito("Homoíne", inhambane);
        salvarBairros(homoine, "Homoíne Sede","Mutuambiti");

        Distrito inharrime = salvarDistrito("Inharrime", inhambane);
        salvarBairros(inharrime, "Inharrime Sede","Mocumbela");

        Distrito inhassoro = salvarDistrito("Inhassoro", inhambane);
        salvarBairros(inhassoro, "Inhassoro Sede","Vilankulo direction");

        Distrito jangamo = salvarDistrito("Jangamo", inhambane);
        salvarBairros(jangamo, "Jangamo Sede","Quissico direction");

        Distrito mabote = salvarDistrito("Mabote", inhambane);
        salvarBairros(mabote, "Mabote Sede");

        Distrito massinga = salvarDistrito("Massinga", inhambane);
        salvarBairros(massinga, "Massinga Sede","Mavume");

        Distrito maxixe = salvarDistrito("Maxixe", inhambane);
        salvarBairros(maxixe,
                "Goto","Maxixe Cidade","Morrumbene direction","Ponela","Quinze");

        Distrito morrumbene = salvarDistrito("Morrumbene", inhambane);
        salvarBairros(morrumbene, "Morrumbene Sede","Morrumbene Litoral");

        Distrito panda = salvarDistrito("Panda", inhambane);
        salvarBairros(panda, "Mapinhane","Panda Sede");

        Distrito vilankulo = salvarDistrito("Vilankulo", inhambane);
        salvarBairros(vilankulo, "Chibuene","Mapinhane","Vilankulo Cidade","Zinhave");

        Distrito zavala = salvarDistrito("Zavala", inhambane);
        salvarBairros(zavala, "Quissico","Zavala Sede","Zibungo");

        // =====================================================================
        // SOFALA
        // =====================================================================
        Provincia sofala = salvarProvincia("Sofala");

        Distrito beira = salvarDistrito("Beira", sofala);
        salvarBairros(beira,
                "Chaimite","Chingussura","Chota","Esturro","Inhamizua",
                "Macuti","Manga","Matacuane","Maquinino","Mudzimu",
                "Munhava","Palmeiras","Pioneiros","Ponta Gêa");

        Distrito buzi = salvarDistrito("Búzi", sofala);
        salvarBairros(buzi, "Búzi Sede","Estaquinha","Nova Sofala");

        Distrito caia = salvarDistrito("Caia", sofala);
        salvarBairros(caia, "Caia Sede","Murraça","Sena");

        Distrito chemba = salvarDistrito("Chemba", sofala);
        salvarBairros(chemba, "Chemba Sede","Mutarara direction");

        Distrito cheringoma = salvarDistrito("Cheringoma", sofala);
        salvarBairros(cheringoma, "Inhaminga","Tica","Nhamatanda direction");

        Distrito chibabava = salvarDistrito("Chibabava", sofala);
        salvarBairros(chibabava, "Chibabava Sede","Ndeja");

        Distrito dondo = salvarDistrito("Dondo", sofala);
        salvarBairros(dondo, "Dondo Cidade","Dondo Sede");

        Distrito gorongosa = salvarDistrito("Gorongosa", sofala);
        salvarBairros(gorongosa, "Gorongosa Sede","Vunduzi");

        Distrito machanga = salvarDistrito("Machanga", sofala);
        salvarBairros(machanga, "Bunga","Machanga Sede");

        Distrito maringueSofala = salvarDistrito("Marínguè", sofala);
        salvarBairros(maringueSofala, "Marínguè Sede","Nhansipfuine");

        Distrito marromeu = salvarDistrito("Marromeu", sofala);
        salvarBairros(marromeu, "Luabo","Marromeu Sede","Muanza direction");

        Distrito muanza = salvarDistrito("Muanza", sofala);
        salvarBairros(muanza, "Muanza Sede");

        Distrito nhamatanda = salvarDistrito("Nhamatanda", sofala);
        salvarBairros(nhamatanda, "Muda","Nhamatanda Sede","Tica");

        // =====================================================================
        // MANICA
        // =====================================================================
        Provincia manica = salvarProvincia("Manica");

        Distrito barue = salvarDistrito("Báruè", manica);
        salvarBairros(barue, "Catandica","Macossa direction","Nhansimba");

        Distrito chimoio = salvarDistrito("Chimoio", manica);
        salvarBairros(chimoio,
                "7 de Abril","Bairro Nhamaonha","Bairro Vila Nova","Chissui",
                "Eduardo Mondlane","Ferroviário","Matsinho","Mpadzane",
                "Nação","Socone","Zobué direction");

        Distrito gondola = salvarDistrito("Gondola", manica);
        salvarBairros(gondola, "Gondola Sede","Inchope","Mudada","Vila Barragem");

        Distrito guro = salvarDistrito("Guro", manica);
        salvarBairros(guro, "Doa","Guro Sede","Mucodza");

        Distrito macate = salvarDistrito("Macate", manica);
        salvarBairros(macate, "Macate Sede","Nhacolo");

        Distrito machaze = salvarDistrito("Machaze", manica);
        salvarBairros(machaze, "Machaze Sede","Mossurize direction");

        Distrito macossa = salvarDistrito("Macossa", manica);
        salvarBairros(macossa, "Macossa Sede");

        Distrito manicaDist = salvarDistrito("Manica", manica);
        salvarBairros(manicaDist, "Machipanda","Manica Cidade","Penhalonga");

        Distrito mossurize = salvarDistrito("Mossurize", manica);
        salvarBairros(mossurize, "Espungabera","Mossurize Sede");

        Distrito sussundenga = salvarDistrito("Sussundenga", manica);
        salvarBairros(sussundenga, "Rotanda","Sussundenga Sede");

        Distrito tambara = salvarDistrito("Tambara", manica);
        salvarBairros(tambara, "Tambara Sede");

        Distrito vanduzi = salvarDistrito("Vanduzi", manica);
        salvarBairros(vanduzi, "Mavonde","Vanduzi Sede");

        // =====================================================================
        // TETE
        // =====================================================================
        Provincia tete = salvarProvincia("Tete");

        Distrito angonia = salvarDistrito("Angónia", tete);
        salvarBairros(angonia, "Domué","Lifidzi","Ulongué");

        Distrito cahoraBassa = salvarDistrito("Cahora-Bassa", tete);
        salvarBairros(cahoraBassa, "Chitima","Songo","Zobué");

        Distrito changara = salvarDistrito("Changara", tete);
        salvarBairros(changara, "Changara Sede","Luenha","Mphende direction");

        Distrito chifunde = salvarDistrito("Chifunde", tete);
        salvarBairros(chifunde, "Chifunde Sede");

        Distrito chiuta = salvarDistrito("Chiúta", tete);
        salvarBairros(chiuta, "Chiúta Sede");

        Distrito doa = salvarDistrito("Doa", tete);
        salvarBairros(doa, "Doa Sede","Zóbuè direction");

        Distrito macanga = salvarDistrito("Macanga", tete);
        salvarBairros(macanga, "Macanga Sede");

        Distrito magoe = salvarDistrito("Magoé", tete);
        salvarBairros(magoe, "Magoé Sede","Mphende","Zumbo direction");

        Distrito marara = salvarDistrito("Marara", tete);
        salvarBairros(marara, "Marara Sede","Moatize direction");

        Distrito moatize = salvarDistrito("Moatize", tete);
        salvarBairros(moatize, "Moatize Cidade","Revuboè","Chipanga");

        Distrito mutarara = salvarDistrito("Mutarara", tete);
        salvarBairros(mutarara, "Chiramba","Inhangoma","Mutarara Sede");

        Distrito teteDist = salvarDistrito("Tete", tete);
        salvarBairros(teteDist,
                "Bairro Samora Machel","Chingodzi","Degue","Interlago",
                "Manyanga","Matenga","Matundo","Mossagala",
                "Mpadue","Nhartcha","Tete Cidade");

        Distrito tsangano = salvarDistrito("Tsangano", tete);
        salvarBairros(tsangano, "Mwanza direction","Tsangano Sede");

        Distrito zumbo = salvarDistrito("Zumbo", tete);
        salvarBairros(zumbo, "Kanyemba","Zumbo Sede");

        // =====================================================================
        // ZAMBÉZIA
        // =====================================================================
        Provincia zambezia = salvarProvincia("Zambézia");

        Distrito altoMolucue = salvarDistrito("Alto Molócuè", zambezia);
        salvarBairros(altoMolucue, "Alto Molócuè Sede","Nauela");

        Distrito chinde = salvarDistrito("Chinde", zambezia);
        salvarBairros(chinde, "Chinde Sede","Marromeu direction");

        Distrito derre = salvarDistrito("Derre", zambezia);
        salvarBairros(derre, "Derre Sede");

        Distrito gile = salvarDistrito("Gilé", zambezia);
        salvarBairros(gile, "Gilé Sede","Nauela direction");

        Distrito gurue = salvarDistrito("Gurué", zambezia);
        salvarBairros(gurue, "Gurué Cidade","Lioma","Alto Molócuè direction");

        Distrito ile = salvarDistrito("Ile", zambezia);
        salvarBairros(ile, "Ile Sede","Nauela","Regone");

        Distrito inhassunge = salvarDistrito("Inhassunge", zambezia);
        salvarBairros(inhassunge, "Inhassunge Sede","Quelimane direction");

        Distrito lugela = salvarDistrito("Lugela", zambezia);
        salvarBairros(lugela, "Lugela Sede","Mocuba direction");

        Distrito maganjaCosta = salvarDistrito("Maganja da Costa", zambezia);
        salvarBairros(maganjaCosta, "Maganja da Costa Sede","Mocuba direction");

        Distrito milange = salvarDistrito("Milange", zambezia);
        salvarBairros(milange, "Milange Sede","Muloza");

        Distrito mocuba = salvarDistrito("Mocuba", zambezia);
        salvarBairros(mocuba, "Mocuba Cidade","Mocuba Sede","Nauela direction");

        Distrito mocubela = salvarDistrito("Mocubela", zambezia);
        salvarBairros(mocubela, "Mocubela Sede");

        Distrito molumbo = salvarDistrito("Molumbo", zambezia);
        salvarBairros(molumbo, "Molumbo Sede");

        Distrito mopeia = salvarDistrito("Mopeia", zambezia);
        salvarBairros(mopeia, "Mopeia Sede","Mopeia Litoral");

        Distrito morrumbala = salvarDistrito("Morrumbala", zambezia);
        salvarBairros(morrumbala, "Morrumbala Sede","Chiromo direction");

        Distrito namacurra = salvarDistrito("Namacurra", zambezia);
        salvarBairros(namacurra, "Macuse","Namacurra Sede","Nicoadala direction");

        Distrito namarroi = salvarDistrito("Namarroi", zambezia);
        salvarBairros(namarroi, "Namarroi Sede");

        Distrito nicoadala = salvarDistrito("Nicoadala", zambezia);
        salvarBairros(nicoadala, "Nicoadala Sede","Quelimane direction");

        Distrito pebane = salvarDistrito("Pebane", zambezia);
        salvarBairros(pebane, "Moebase","Pebane Sede");

        Distrito quelimane = salvarDistrito("Quelimane", zambezia);
        salvarBairros(quelimane,
                "Chabeco","Coalane","Estação","Icídua","Madal",
                "Manhaua","Namuinho","Quelimane Cidade","Sangariveira");

        // =====================================================================
        // NAMPULA
        // =====================================================================
        Provincia nampula = salvarProvincia("Nampula");

        Distrito angoche = salvarDistrito("Angoche", nampula);
        salvarBairros(angoche, "Angoche Cidade","António Enes","Liúpo direction");

        Distrito erati = salvarDistrito("Eráti", nampula);
        salvarBairros(erati, "Nacucha","Namapa","Quixaxe");

        Distrito ilhaMocambique = salvarDistrito("Ilha de Moçambique", nampula);
        salvarBairros(ilhaMocambique, "Estanquinha","Lumbo","Mossuril direction","Nkhalakatha");

        Distrito lalaua = salvarDistrito("Lalaua", nampula);
        salvarBairros(lalaua, "Lalaua Sede","Mutali direction");

        Distrito larde = salvarDistrito("Larde", nampula);
        salvarBairros(larde, "Larde Sede","Moma direction");

        Distrito liupo = salvarDistrito("Liúpo", nampula);
        salvarBairros(liupo, "Liúpo Sede");

        Distrito malema = salvarDistrito("Malema", nampula);
        salvarBairros(malema, "Corrane","Malema Sede","Mutuali");

        Distrito meconta = salvarDistrito("Meconta", nampula);
        salvarBairros(meconta, "Meconta Sede","Namialo");

        Distrito mecuburi = salvarDistrito("Mecubúri", nampula);
        salvarBairros(mecuburi, "Mecubúri Sede","Muecate direction");

        Distrito memba = salvarDistrito("Memba", nampula);
        salvarBairros(memba, "Memba Sede","Nacala direction");

        Distrito mogincual = salvarDistrito("Mogincual", nampula);
        salvarBairros(mogincual, "Mogincual Sede","Namige");

        Distrito mogovolas = salvarDistrito("Mogovolas", nampula);
        salvarBairros(mogovolas, "Mogovolas Sede","Nametil","Nicoadala direction");

        Distrito moma = salvarDistrito("Moma", nampula);
        salvarBairros(moma, "Moma Praia","Moma Sede","Parapato");

        Distrito monapo = salvarDistrito("Monapo", nampula);
        salvarBairros(monapo, "Monapo Sede","Namialo","Netia");

        Distrito mossuril = salvarDistrito("Mossuril", nampula);
        salvarBairros(mossuril, "Chocas Mar","Mossuril Sede","Naróia");

        Distrito muecate = salvarDistrito("Muecate", nampula);
        salvarBairros(muecate, "Muecate Sede");

        Distrito murrupula = salvarDistrito("Murrupula", nampula);
        salvarBairros(murrupula, "Murrupula Sede","Nauela direction");

        Distrito nacalaVelha = salvarDistrito("Nacala-a-Velha", nampula);
        salvarBairros(nacalaVelha, "Nacala-a-Velha Sede","Sancul");

        Distrito nacalaPorto = salvarDistrito("Nacala Porto", nampula);
        salvarBairros(nacalaPorto,
                "Bairro Cimento","Muatala","Nacala Cidade","Nacala Porto",
                "Nacuco","Natikiri","Rapale direction");

        Distrito nampulaDist = salvarDistrito("Nampula", nampula);
        salvarBairros(nampulaDist,
                "Anchilo","Marrere","Muhala","Muahivire","Mutauanha",
                "Napipine","Natikiri","Nampula Cidade","Macomia direction");

        Distrito rapale = salvarDistrito("Rapale", nampula);
        salvarBairros(rapale, "Macomia direction","Rapale Sede");

        Distrito ribaue = salvarDistrito("Ribáuè", nampula);
        salvarBairros(ribaue, "Iapala","Ribáuè Sede");

        // =====================================================================
        // CABO DELGADO
        // =====================================================================
        Provincia caboDelgado = salvarProvincia("Cabo Delgado");

        Distrito ancuabe = salvarDistrito("Ancuabe", caboDelgado);
        salvarBairros(ancuabe, "Ancuabe Sede","Balama direction","Ocua");

        Distrito balama = salvarDistrito("Balama", caboDelgado);
        salvarBairros(balama, "Balama Sede","Namuno direction");

        Distrito chiure = salvarDistrito("Chiúre", caboDelgado);
        salvarBairros(chiure, "Chiúre Novo","Chiúre Sede","Mazeze");

        Distrito ibo = salvarDistrito("Ibo", caboDelgado);
        salvarBairros(ibo, "Ibo Sede","Quirimba");

        Distrito macomia = salvarDistrito("Macomia", caboDelgado);
        salvarBairros(macomia, "Macomia Sede","Quiterajo");

        Distrito mecufi = salvarDistrito("Mecúfi", caboDelgado);
        salvarBairros(mecufi, "Mecúfi Sede","Pemba direction");

        Distrito meluco = salvarDistrito("Meluco", caboDelgado);
        salvarBairros(meluco, "Meluco Sede");

        Distrito metuge = salvarDistrito("Metuge", caboDelgado);
        salvarBairros(metuge, "Metuge Sede","Pemba direction");

        Distrito mocimboaPraia = salvarDistrito("Mocímboa da Praia", caboDelgado);
        salvarBairros(mocimboaPraia,
                "Mocímboa da Praia Sede","Chomba","Pangane");

        Distrito montepuez = salvarDistrito("Montepuez", caboDelgado);
        salvarBairros(montepuez,
                "Bairro Cimento","Montepuez Cidade","Montepuez Sede","Nairoto");

        Distrito mueda = salvarDistrito("Mueda", caboDelgado);
        salvarBairros(mueda, "Mueda Sede","Nangade direction","Nangololo");

        Distrito muidumbe = salvarDistrito("Muidumbe", caboDelgado);
        salvarBairros(muidumbe, "Muidumbe Sede");

        Distrito namuno = salvarDistrito("Namuno", caboDelgado);
        salvarBairros(namuno, "Namuno Sede","Ocua direction");

        Distrito nangade = salvarDistrito("Nangade", caboDelgado);
        salvarBairros(nangade, "Muatide","Nangade Sede");

        Distrito palma = salvarDistrito("Palma", caboDelgado);
        salvarBairros(palma, "Mizano","Palma Sede","Quitunda");

        Distrito pemba = salvarDistrito("Pemba", caboDelgado);
        salvarBairros(pemba,
                "Bairro Cariaco","Bairro Ingonane","Cariacó","Cimento",
                "Natite","Paquitequete","Pemba Cidade","Wimbe");

        Distrito quissanga = salvarDistrito("Quissanga", caboDelgado);
        salvarBairros(quissanga, "Bilibiza","Quissanga Sede");

        // =====================================================================
        // NIASSA
        // =====================================================================
        Provincia niassa = salvarProvincia("Niassa");

        Distrito chimbonila = salvarDistrito("Chimbonila", niassa);
        salvarBairros(chimbonila, "Chimbonila Sede","Lichinga direction");

        Distrito cuamba = salvarDistrito("Cuamba", niassa);
        salvarBairros(cuamba, "Cuamba Cidade","Cuamba Sede","Mutuali direction");

        Distrito lago = salvarDistrito("Lago", niassa);
        salvarBairros(lago, "Cobué","Meponda","Metangula");

        Distrito lichinga = salvarDistrito("Lichinga", niassa);
        salvarBairros(lichinga,
                "Chiuangua","Lichinga Cidade","Massangulo",
                "Mitande","Namacula","Nauela direction");

        Distrito majune = salvarDistrito("Majune", niassa);
        salvarBairros(majune, "Majune Sede","Maúa direction");

        Distrito mandimba = salvarDistrito("Mandimba", niassa);
        salvarBairros(mandimba, "Mandimba Sede","Marrupa direction");

        Distrito marrupa = salvarDistrito("Marrupa", niassa);
        salvarBairros(marrupa, "Marrupa Sede","Nipepe");

        Distrito maua = salvarDistrito("Maúa", niassa);
        salvarBairros(maua, "Maúa Sede","Muembe direction");

        Distrito mavago = salvarDistrito("Mavago", niassa);
        salvarBairros(mavago, "Mavago Sede");

        Distrito mecanhelas = salvarDistrito("Mecanhelas", niassa);
        salvarBairros(mecanhelas, "Mecanhelas Sede","Metarica direction");

        Distrito mecula = salvarDistrito("Mecula", niassa);
        salvarBairros(mecula, "Mecula Sede");

        Distrito metarica = salvarDistrito("Metarica", niassa);
        salvarBairros(metarica, "Metarica Sede");

        Distrito muembe = salvarDistrito("Muembe", niassa);
        salvarBairros(muembe, "Muembe Sede");

        Distrito ngauma = salvarDistrito("N'gauma", niassa);
        salvarBairros(ngauma, "N'gauma Sede");

        Distrito sanga = salvarDistrito("Sanga", niassa);
        salvarBairros(sanga, "Sanga Sede");

        System.out.println(">>> [DataInitializer] Dados geográficos de Moçambique inseridos com sucesso!");
        System.out.println(">>> 11 Províncias | " + distritoRepo.count() + " Distritos | " + bairroRepo.count() + " Bairros");
    }
    // ─── Helpers ────────────────────────────────────────────────────────────

    private Provincia salvarProvincia(String nome) {
        Provincia p = new Provincia();
        p.setNome(nome);
        return provinciaRepo.save(p);
    }

    private Distrito salvarDistrito(String nome, Provincia provincia) {
        Distrito d = new Distrito();
        d.setNome(nome);
        d.setProvincia(provincia);
        return distritoRepo.save(d);
    }

    private void salvarBairros(Distrito distrito, String... nomes) {
        for (String nome : nomes) {
            Bairro b = new Bairro();
            b.setNome(nome);
            b.setDistrito(distrito);
            bairroRepo.save(b);
        }
    }
}
