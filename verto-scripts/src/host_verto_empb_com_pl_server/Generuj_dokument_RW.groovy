package host_verto_empb_com_pl_server;
import pl.com.stream.lib.commons.math.Decimal
import pl.com.stream.verto.whm.warehousedocument.server.pub.item.dispatch.WhmDocDispatchItemDto
import pl.com.stream.verto.whm.warehousedocument.server.pub.item.dispatch.WhmDocDispatchItemService
import pl.com.stream.verto.whm.warehousedocument.server.pub.item.receiving.WhmDocReceivingItemDto
import pl.com.stream.verto.whm.warehousedocument.server.pub.item.receiving.WhmDocReceivingItemService
import pl.com.stream.verto.whm.warehousedocument.server.pub.main.WhmDocumentDto
import pl.com.stream.verto.whm.warehousedocument.server.pub.main.WhmDocumentService
class Generuj_dokument_RW extends pl.com.stream.next.asen.common.groovy.ServerScriptEnv {
    def script() {






        /* Skrypt dodaje dokument PW wraz z pozycją
         * Funcje skryptu
         * 
         * 1. Wygenerowanie rozchodu z podanej partii
         */



        Long idWhmDocReceivingItem = inParams.idWhmDocReceivingItem;
        Long idGood =  inParams.idGood;
        Long idCostCreatePlace = inParams.idCostCreatePlace;
        Decimal quantity = inParams.quantity;
        Long idWhmDocumentDef = inParams.idWhmDocumentDef;
        Long idWarehouse = inParams.idWarehouse;
        Long idProject = inParams.idProject;
        Long idProjectTask = inParams.idProjectTask;


        WhmDocumentService whmDocumentService = context.getService(WhmDocumentService.class);

        WhmDocumentDto whmDocumentDto = new WhmDocumentDto();
        whmDocumentDto.idDocumentDefinition = idWhmDocumentDef;
        whmDocumentDto.idWarehouse = idWarehouse;
        whmDocumentDto.idCostCreatePlace = idCostCreatePlace;
        whmDocumentDto.idProject = idProject;
        whmDocumentDto.idSchedule = idProjectTask;

        whmDocumentDto = whmDocumentService.init(whmDocumentDto);
        Long idWhmDoc = whmDocumentService.insert(whmDocumentDto);


        //Odszukanie partii
        WhmDocReceivingItemService whmDocReceivingItemService = context.getService(WhmDocReceivingItemService.class);


        WhmDocDispatchItemService whmDocDispatchItemService = context.getService(WhmDocDispatchItemService.class);

        WhmDocReceivingItemDto whmDocReceivingItemDto = whmDocReceivingItemService.find(idWhmDocReceivingItem);

        def query = """
                    Select ps.idPartStock from PartStock ps where ps.part.idPart = :idPart     
                    """;

        def List<Long> ids = dm.executeQuery(query, ["idPart":whmDocReceivingItemDto.idPart]);

        Long idPart;

        if (ids.size()>0) {
            idPart = ids.get(0);
        }



        WhmDocDispatchItemDto whmDocDispatchItemDto = new WhmDocDispatchItemDto();

        whmDocDispatchItemDto.idWhmDocument = idWhmDoc;
        whmDocDispatchItemDto.idGood = idGood;
        whmDocDispatchItemDto.quantity = quantity;
        whmDocDispatchItemDto.idPartStock = idPart;

        whmDocDispatchItemDto = whmDocDispatchItemService.init(whmDocDispatchItemDto);
        Long idWhmDocDispatchItem = whmDocDispatchItemService.insert(whmDocDispatchItemDto);

        whmDocumentService.confirm(idWhmDoc);

        outParams.idWhmDocDispatch = idWhmDoc;
    }
}