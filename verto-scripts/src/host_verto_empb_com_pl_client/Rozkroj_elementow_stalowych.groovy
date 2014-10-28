package host_verto_empb_com_pl_client;
import pl.com.stream.lib.commons.math.Decimal
import pl.com.stream.next.asen.common.groovy.api.common.GroovyOutputParams
import pl.com.stream.next.plugin.database.pub.lib.simpledatasource.Field
import pl.com.stream.next.plugin.database.pub.lib.simpledatasource.FieldType
import pl.com.stream.next.plugin.database.pub.lib.simpledatasource.FieldValueChangeListener
import pl.com.stream.next.plugin.database.pub.lib.simpledatasource.Fields
import pl.com.stream.next.plugin.database.pub.lib.simpledatasource.State
import pl.com.stream.next.plugin.vedas.pub.lib.simpleeditwindow.SimpleEditWindow
import pl.com.stream.verto.cmm.attribute.server.pub.value.AttributeValueService
import pl.com.stream.verto.cmm.good.server.pub.main.dto.GoodDto
import pl.com.stream.verto.cmm.good.server.pub.main.service.GoodService
import pl.com.stream.verto.whm.dictionary.server.pub.part.PartDto
import pl.com.stream.verto.whm.dictionary.server.pub.part.PartService
import pl.com.stream.verto.whm.warehousedocument.server.pub.item.dispatch.WhmDocDispatchItemDto
import pl.com.stream.verto.whm.warehousedocument.server.pub.item.dispatch.WhmDocDispatchItemService
import pl.com.stream.verto.whm.warehousedocument.server.pub.item.receiving.WhmDocReceivingItemDto
import pl.com.stream.verto.whm.warehousedocument.server.pub.item.receiving.WhmDocReceivingItemService
import pl.com.stream.verto.whm.warehousedocument.server.pub.main.WhmDocumentDto
import pl.com.stream.verto.whm.warehousedocument.server.pub.main.WhmDocumentService
class Rozkroj_elementow_stalowych extends pl.com.stream.verto.adm.asen.tools.client.pub.script.api.ClinetScriptEnv {
    def script() {


















        /* Zadaniem skryptu jest przyspieszenie procesu rozkroju elementow stalowych 
         * Operacje wykonywanie przez skrypt.
         * 
         * 1. Pobranie wartości pozycji dokumentu RW i wyliczenie średniej ceny za jednostę.
         * 2. Sprawdzenie w jakiej grupie materiałowej znajduje się materiał i jaki ma gatunek. // W kolejnej wersji
         * 3. Utworzenie nowego indeksu odpadu użytechnego dla ciętego elementu o ile kartoteka już nie istnieje. // W kolejnej wersji
         * 4. Wyświetlenie okna z polami do wpisania wymiarów, ilości KG, ilości sztuk i przelicznika. 
         * 5. Możliwość określenia na oknie edycyjnym parametru "Wydanie na produkcję".
         * 6. Utworzenie dokumentu PW na z odpowiednimi parametrami dostawy ilością i  wyceną elementów pociętych.
         * 7. Wygenerowanie dokumentu RW dla pozycji z zaznaczonym parametrem "Wydanie na produkcję". 
         * 8. Wyświetlenie raportu podsumowywującego.
         *  
         */


        Long idWhmDocDispatchItem = inParams.idWhmDocDispatchItem;

        Long idAttributeDefGroup = 100005L // Cecha kartoteki grupa materiałowa
        Long idAttributeDefSpecies = 101200L // Cecha kartoteki grupa materiałowa
        Long idWhmDocDef = 100067L; //Definicja dokumentu PW
        Long idWhmDocDefRw = 100071L; //Definicja dokumentu RW
        Long idWarehouse = 100000L  //Definicja magazynu

        final String notAttributeVlue = 'Kartoteka nie ma wskazanych atrybutów  grupa lub gatunek. Nie można odnaleźć lub utowrzyć odpadu użytecznego';

        String endNote ="Podsumowanie \n";

        WhmDocDispatchItemService whmDocDispatchItemService = context.getService(WhmDocDispatchItemService.class);


        WhmDocDispatchItemDto whmDocDispatchItemDto = whmDocDispatchItemService.find(idWhmDocDispatchItem);

        if (whmDocDispatchItemDto.equals(null)) {
            return;
        }



        //Pobranie ceny

        Decimal dispatchPrice =  whmDocDispatchItemDto.warehousePrice;

        //Pobranie miejsca powstawania kosztów

        WhmDocumentService whmDocumentService = context.getService(WhmDocumentService.class);

        WhmDocumentDto whmDocumentDto = whmDocumentService.find(whmDocDispatchItemDto.idWhmDocument);
        Long idCostCreatePlace = whmDocumentDto.idCostCreatePlace;

        //Pobranie nazwy kartoteki

        //Sprawdzenie grupy towarowej i gatunku materiału - do wykonania w kolejnej wersji

        /* Long idGoodDispatch = whmDocDispatchItemDto.idGood;
         GoodService goodService = context.getService(GoodService.class);
         GoodDto goodDto = goodService.find(idGoodDispatch);
         Long idAttributeSubject = goodDto.idAttributeSubject;
         AttributeValueService attributeValueService = context.getService(AttributeValueService.class);
         Long idAttributeValueGroup = attributeValueService.findIdBySubjectAndDef(idAttributeSubject, idAttributeDefGroup);
         Long idAttributeValueSpecies = attributeValueService.findIdBySubjectAndDef(idAttributeSubject, idAttributeDefSpecies);
         AttributeValueDto  attributeValueGroupDto = attributeValueService.find(idAttributeValueGroup);
         AttributeValueDto  attributeValueSpeciesDto = attributeValueService.find(idAttributeValueSpecies);
         if ( ( attributeValueGroupDto.stringValue.isEmpty() ) || (attributeValueSpeciesDto.stringValue.isEmpty())) {
         throw new BusinessOperationException(notAttributeVlue);
         }
         //Wyszukanie kartoteki po nazwie
         String longName = attributeValueGroupDto.stringValue + ' ' + attributeValueSpeciesDto.stringValue;
         String hql = 'Select g.good.idGood from GoodData g where g.longName =:longName';
         List<Long> ids = database.executeQuery(hql, ["longName":longName]);
         Long idGoodReceiving = ids.get(0);
         if (idGoodReceiving.SIZE == 0) {
         //Utworzenie nowego indeksu
         }
         */

        //Utworzenie okna edycyjengo

        final ID_OKNA = "asoft.rozkrojStali";

        SimpleEditWindow window = dataWindow.create(ID_OKNA);

        window.setWindowTitle("Rozkrój elementów stalowych");
        window.setBanerTitle("Rozkrój elementów stalowych");
        window.setBanerMessage("Rozkrój : " + whmDocDispatchItemDto.shortName);

        window.setCancelButtonLabel("Anuluj");
        window.setExecuteButtonLabel("Generuj");

        window.addField('good', 'Materiał', FieldType.Dictionary).setDictionary('idGood').setState(State.Required).setValue(101201L);
        window.addField('dimension_1', 'Wymiar').setState(State.Required);
        window.addField('quantity_1', 'Ilość Kg',FieldType.Numeric).setState(State.Editable);
        window.addField('quantityPsc_1', 'Ilość szt/m',FieldType.Numeric).setState(State.NotEnabled);
        window.addField('calculate_1', 'Przelicznik',FieldType.Numeric).setState(State.NotEnabled);
        window.addField('onProduction_1', 'Wydaj na produkcję', FieldType.Boolean).setState(State.Editable).setValue(false);

        window.addField('dimension_2', 'Wymiar').setState(State.Editable);;
        window.addField('quantity_2', 'Ilość Kg',FieldType.Numeric).setState(State.Editable);
        window.addField('quantityPsc_2', 'Ilość szt/m',FieldType.Numeric).setState(State.NotEnabled);
        window.addField('calculate_2', 'Przelicznik',FieldType.Numeric).setState(State.NotEnabled);
        window.addField('onProduction_2', 'Wydaj na produkcję', FieldType.Boolean).setState(State.Editable).setValue(false);

        /*def validatorGood = {Fields fields, ValidationResultBuilder builder ->
         def Long good = fields.get('good').getValue();
         if (good.SIZE == 0) {
         builder.addFieldValidationError('good','Należy wskazać materiał');
         }
         } as Validator
         def validatorDimension_1 = {Fields fields, ValidationResultBuilder builder ->
         def String dimension = fields.get('dimension_1').getValue();
         if (dimension.length() > 0) {
         builder.addFieldValidationError('dimension_1','Proszę podać wymiar');
         }
         } as Validator*/


        def dimension_1Listener = {Fields fields, Field changedField ->
            String hasDimension = changedField.getValue();
            if (hasDimension.equals(null)) {
                hasDimension = '';
            }

            if (hasDimension.length() > 0) {
                fields.get('quantity_1').setState(State.Required)
                fields.get('quantityPsc_1').setState(State.Editable);
                fields.get('calculate_1').setState(State.Editable);
            }else{
                fields.get('quantity_1').setState(State.Editable);
                fields.get('quantityPsc_1').setState(State.NotEnabled);
                fields.get('calculate_1').setState(State.NotEnabled);
            }


        } as FieldValueChangeListener;


        def dimension_2Listener = {Fields fields, Field changedField ->
            String hasDimension = changedField.getValue();
            if (hasDimension.equals(null)) {
                hasDimension = '';
            }

            if (hasDimension.length() > 0) {
                fields.get('quantity_2').setState(State.Required)
                fields.get('quantityPsc_2').setState(State.Editable);
                fields.get('calculate_2').setState(State.Editable);
            }else{
                fields.get('quantity_2').setState(State.Editable);
                fields.get('quantityPsc_2').setState(State.NotEnabled);
                fields.get('calculate_2').setState(State.NotEnabled);
            }


        } as FieldValueChangeListener;


        Field dimension_1Field = window.getFields().dimension_1;
        Field dimension_2Field = window.getFields().dimension_2;

        dimension_1Field.addFieldValueChangeListener(dimension_1Listener);

        dimension_2Field.addFieldValueChangeListener(dimension_2Listener);

        String basicDataPanelLayout = """
        <Row>
            <Component id="good" class="label" expand="0" template="top"/>
        </Row>
        <Row>
            <Component id="good" class="field" expand ="50"/>
        </Row>
        <Row>
            <Component id="dimension_1"  class="label"   template="top"/> 
            <Component id="onProduction_1" class="label" expand="0" template="right" occupyx="1"/> 
            <Component id="onProduction_1" class="field" expand="0" align="right" occupyx="1"/>   
        </Row>
        <Row>
            <Component id="dimension_1"  class="field" expand="40"/> 
        </Row>
        <Row>
            <Component id="quantity_1"  class="label"   template="top"/> 
            <Component id="quantityPsc_1" class="label"  template="top"/>
            <Component id="calculate_1"  class="label"  template="top"/>   
        </Row>
        <Row>
            <Component id="quantity_1"  class="field"  /> 
            <Component id="quantityPsc_1"  class="field" expand="0"/>  
            <Component id="calculate_1"  class="field" expand="0"/> 
        </Row>          
        <Component id="seperator1" class="separator"/>
        <Row>
            <Component id="dimension_2"  class="label"   template="top"/> 
            <Component id="onProduction_2" class="label" expand="0" template="right" occupyx="1"/> 
            <Component id="onProduction_2" class="field" expand="0" align="right" occupyx="1"/>  
        </Row>
        <Row>
            <Component id="dimension_2"  class="field" expand="40"/> 
        </Row>
        <Row>
            <Component id="quantity_2"  class="label"   template="top"/> 
            <Component id="quantityPsc_2" class="label"  template="top"/>
            <Component id="calculate_2"  class="label"  template="top"/>   
        </Row>
        <Row>
            <Component id="quantity_2"  class="field"  /> 
            <Component id="quantityPsc_2"  class="field" expand="0"/>  
            <Component id="calculate_2"  class="field" expand="0"/> 
        </Row>         
        """
        window.addPanel("Dane podstawowe", basicDataPanelLayout);


        window.setShowQuestionBeforeEscapeWhenDataWasModified(false);

        if(window.run()){

            //Wygenerowanie dokumentu PW



            Long idGood = window.getFieldsValue().good;
            String dimension_1 =  window.getFieldsValue().dimension_1;
            Decimal quantity_1 =  window.getFieldsValue().quantity_1;
            Boolean onProduction_1 = window.getFieldsValue().onProduction_1;
            Boolean onProduction_2 = window.getFieldsValue().onProduction_2

            Decimal quantityPsc_1 =  window.getFieldsValue().quantityPsc_1;
            if (quantityPsc_1.equals(null)) {
                quantityPsc_1 = new Decimal(0);
            }

            Decimal calculate_1 =  window.getFieldsValue().calculate_1;
            if (calculate_1.equals(null)) {
                calculate_1 = new Decimal(0);
            }

            String dimension_2 =  window.getFieldsValue().dimension_2;
            Decimal quantity_2 =  window.getFieldsValue().quantity_2;

            Decimal quantityPsc_2 =  window.getFieldsValue().quantityPsc_2;
            if (quantityPsc_2.equals(null)) {
                quantityPsc_2 = new Decimal(0);
            }
            Decimal calculate_2 =  window.getFieldsValue().calculate_2;
            if (calculate_2.equals(null)) {
                calculate_2 = new Decimal(0);
            }

            Long idWhmDocReceivingItem_1 = 0L;
            Long idWhmDocReceiving = 0L;

            Long idAttrDimensionDef = 100306L;
            Long idAttrQuantityPscDef = 100006L;
            Long idAttrCalculateDef = 100304L;


            GoodService goodService = context.getService(GoodService.class);
            GoodDto goodDto = goodService.find(idGood);

            String goodName = goodDto.longName;

            //Utworzenie dokumentu PW

            WhmDocumentDto whmDocReceivingDto = new WhmDocumentDto();
            whmDocReceivingDto.idDocumentDefinition = idWhmDocDef;
            whmDocReceivingDto.idWarehouse = idWarehouse;
            whmDocReceivingDto.idCostCreatePlace = idCostCreatePlace;

            whmDocReceivingDto = whmDocumentService.init(whmDocReceivingDto);
            idWhmDocReceiving = whmDocumentService.insert(whmDocReceivingDto);

            endNote =  endNote + 'Wygenerowano dokument przychodu nr : ' + whmDocumentService.find(idWhmDocReceiving).documentNumber + '\n';

            //Dodanie pierwszej pozycji
            String partNumber = idGood.toString() + ' ' + dimension_1;

            WhmDocReceivingItemDto whmDocReceivingItemDto_1 = new WhmDocReceivingItemDto();
            whmDocReceivingItemDto_1.idGood = idGood;
            whmDocReceivingItemDto_1.quantity = quantity_1;
            whmDocReceivingItemDto_1.warehousePrice = dispatchPrice;
            whmDocReceivingItemDto_1.idWhmDocument = idWhmDocReceiving;
            whmDocReceivingItemDto_1.partNumber = partNumber;




            PartService partService = context.getService(PartService.class);


            List idPart = dm.executeSQLQuery('Select p.id_Part from Part_v p where p.part_Number =:partNumber', ['partNumber':partNumber])



            if (idPart.size() > 0) {
                whmDocReceivingItemDto_1.idPart = idPart.get(0);
            }


            WhmDocReceivingItemService whmDocReceivingItemService = context.getService(WhmDocReceivingItemService.class);

            whmDocReceivingItemDto_1 = whmDocReceivingItemService.init(whmDocReceivingItemDto_1);

            idWhmDocReceivingItem_1 =  whmDocReceivingItemService.insert(whmDocReceivingItemDto_1);

            whmDocReceivingItemDto_1 = whmDocReceivingItemService.find(idWhmDocReceivingItem_1);

            endNote =  endNote + 'Dodano pozycje : ' + goodName + ' wymiar: ' + partNumber + '\n';
            //Uzupełnienie cech

            PartDto partDto = partService.find(whmDocReceivingItemDto_1.idPart);

            AttributeValueService attributeValueService = context.getService(AttributeValueService.class);

            attributeValueService.insertAttributeValueBySubjectAndDef(partDto.idAttributeSubject, idAttrDimensionDef, dimension_1);
            attributeValueService.insertAttributeValueBySubjectAndDef(partDto.idAttributeSubject, idAttrQuantityPscDef, quantityPsc_1);
            attributeValueService.insertAttributeValueBySubjectAndDef(partDto.idAttributeSubject, idAttrCalculateDef, calculate_1);


            Long idWhmDocReceivingItem_2 ;

            if (!dimension_2.equals(null)) {


                partNumber = idGood.toString() + ' ' + dimension_2;

                WhmDocReceivingItemDto whmDocReceivingItemDto_2 = new WhmDocReceivingItemDto();
                whmDocReceivingItemDto_2.idGood = idGood;
                whmDocReceivingItemDto_2.quantity = quantity_2;
                whmDocReceivingItemDto_2.warehousePrice = dispatchPrice;
                whmDocReceivingItemDto_2.idWhmDocument = idWhmDocReceiving;
                whmDocReceivingItemDto_2.partNumber = partNumber;

                idPart = dm.executeSQLQuery('Select p.id_Part from Part_v p where p.part_Number =:partNumber', ['partNumber':partNumber])

                if (idPart.size() > 0) {
                    whmDocReceivingItemDto_2.idPart = idPart.get(0);
                }

                whmDocReceivingItemDto_2 = whmDocReceivingItemService.init(whmDocReceivingItemDto_2);
                idWhmDocReceivingItem_2 =  whmDocReceivingItemService.insert(whmDocReceivingItemDto_2);
                whmDocReceivingItemDto_2 = whmDocReceivingItemService.find(idWhmDocReceivingItem_2);

                endNote =  endNote +  'Dodano pozycje : ' + goodName + ' wymiar: ' + partNumber + '\n';

                //Uzupełnienie cech

                partDto = partService.find(whmDocReceivingItemDto_2.idPart);

                attributeValueService.insertAttributeValueBySubjectAndDef(partDto.idAttributeSubject, idAttrDimensionDef, dimension_2);
                attributeValueService.insertAttributeValueBySubjectAndDef(partDto.idAttributeSubject, idAttrQuantityPscDef, quantityPsc_2);
                attributeValueService.insertAttributeValueBySubjectAndDef(partDto.idAttributeSubject, idAttrCalculateDef, calculate_2);


            }

            if (whmDocumentService.isInPreparation(idWhmDocReceiving)) {
                whmDocumentService.confirm(idWhmDocReceiving);
            }



            //Wykonanie rozchodu na produkcję

            if (onProduction_1) {
                GroovyOutputParams outParamsRozchod = script.find('Generuj dokument RW').execute([idWhmDocReceivingItem : idWhmDocReceivingItem_1,
                    idWhmDocumentDef : idWhmDocDefRw, idWarehouse : idWarehouse, idGood : idGood, quantity : quantity_1, idCostCreatePlace : idCostCreatePlace]);


                endNote =  endNote +  'Wygenerowano dokument rozchodu nr : ' +  whmDocumentService.find(outParamsRozchod.idWhmDocDispatch).documentNumber + '\n';

            }

            if (onProduction_2) {
                GroovyOutputParams outParamsRozchod = script.find('Generuj dokument RW').execute([idWhmDocReceivingItem : idWhmDocReceivingItem_2,
                    idWhmDocumentDef : idWhmDocDefRw, idWarehouse : idWarehouse, idGood : idGood, quantity : quantity_2, idCostCreatePlace : idCostCreatePlace]);

                endNote =  endNote +  'Wygenerowano dokument rozchodu nr : ' +  whmDocumentService.find(outParamsRozchod.idWhmDocDispatch).documentNumber + '\n';
            }

            SimpleEditWindow windowM = dataWindow.create(ID_OKNA + '.' +'summary');

            windowM.setWindowTitle("Rozkrój elementów stalowych");
            windowM.setBanerTitle("Rozkrój elementów stalowych");
            windowM.setBanerMessage("Podsumowanie");

            windowM.setCancelButtonLabel("Anuluj");

            windowM.addField('note', 'Podsumowanie', FieldType.Memo).setState(State.NotEditable).setValue(endNote);
            windowM.setShowQuestionBeforeEscapeWhenDataWasModified(false);
            windowM.run();
        }
    }
}