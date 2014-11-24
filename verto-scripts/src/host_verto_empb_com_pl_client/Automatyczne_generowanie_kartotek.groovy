package host_verto_empb_com_pl_client;
import pl.com.stream.next.plugin.database.pub.lib.simpledatasource.FieldType
import pl.com.stream.next.plugin.database.pub.lib.simpledatasource.Fields
import pl.com.stream.next.plugin.database.pub.lib.simpledatasource.State
import pl.com.stream.next.plugin.database.pub.lib.simpledatasource.ValidationResultBuilder
import pl.com.stream.next.plugin.database.pub.lib.simpledatasource.Validator
import pl.com.stream.next.plugin.vedas.pub.lib.simpleeditwindow.SimpleEditWindow
import pl.com.stream.verto.cmm.attribute.server.pub.value.AttributeValueService
import pl.com.stream.verto.cmm.attribute.server.pub.value.WhatUseForEditIdBusinessDictionaryItem
import pl.com.stream.verto.cmm.good.server.pub.main.dto.GoodDto
import pl.com.stream.verto.cmm.good.server.pub.main.service.GoodService
import pl.com.stream.verto.cmm.good.server.pub.partident.PartIdentAttrSubDefDto
import pl.com.stream.verto.cmm.good.server.pub.partident.PartIdentAttrSubDefService
import pl.com.stream.verto.pch.purchasedictionary.server.pub.main.PurchasePlaceService
import pl.com.stream.verto.sup.dictionary.server.pub.main.SupplyPlaceService
class Automatyczne_generowanie_kartotek extends pl.com.stream.verto.adm.asen.tools.client.pub.script.api.ClinetScriptEnv {
    def script() {

        //Identyfikatory cech dla kartotek typu "Materiał"


        enum AttributesPart{
                    SZTUKI(100505L), PRZELICZNIK(100506L), ATEST(100508L), TYPATEST(100509L), WYTOP(100600)

                    private Long idAttribute;

                    AttributesPart(Long idAttribute){
                        this.idAttribute = idAttribute;
                    }

                    Long getIdAttribute(){
                        return idAttribute;
                    }
                }

        enum AttributesPartNotUseGood{
                    WYMIAR(100306L), SZTUKI(100505L), PRZELICZNIK(100506L),  ATEST(100508L), TYPATEST(100509L), WYTOP(100600)

                    private Long idAttribute;

                    AttributesPartNotUseGood(Long idAttribute){
                        this.idAttribute = idAttribute;
                    }

                    Long getIdAttribute(){
                        return idAttribute;
                    }
                }


        final ID_OKNA = "asoft.autoGenKart";

        SimpleEditWindow window = dataWindow.create(ID_OKNA);

        window.setWindowTitle("Automatyczne generowanie kartoteki");
        window.setBanerTitle("Generowanie kartoteki na podstawie atrybutów");


        window.setCancelButtonLabel("Anuluj");
        window.setExecuteButtonLabel("Generuj");

        window.addField("attSubjectDef", "Definicja",FieldType.Dictionary).setDictionary('idBusinessDictionaryList').setValue(100000L).setState(State.Invisible);
        window.addField("idBusinessDictionaryItemTree", "Grupa materiałowa", FieldType.Dictionary).setDictionary('idBusinessDictionaryItemTree');
        window.addField("dimension","Wymiar",FieldType.String).setState(State.Editable);
        window.addField("species","Gatunek",FieldType.String).setState(State.Editable);
        window.addField("unitCode", "Jednostka mairy", FieldType.Dictionary).setDictionary('idUnit').setValue(100000L);
        window.addField("trash","Po cięciu",FieldType.Boolean).setValue(false);
        window.addField("isPurchaseReverseCharge","Odwrotne obciążenie",FieldType.Boolean).setValue(false);

        String basicDataPanelLayout = """
            
            <Row>
                <Component id="idBusinessDictionaryItemTree"  >
                   <Property name="idBusinessDictionaryList" binding="field.main.attSubjectDef" /> 
                </Component>               
            </Row>
            <Row>
                <Component id="dimension" />
            </Row>
            <Row>
                <Component id="species" />
            </Row>
            <Row>
                <Component id="unitCode" />
            </Row>
            <Row>
                <Component id="trash"  />                 
            </Row> 
            <Row>           
                <Component id="isPurchaseReverseCharge"  />                
            </Row>
           
          """

        window.addPanel("Dane podstawowe", basicDataPanelLayout);

        window.setShowQuestionBeforeEscapeWhenDataWasModified(false);

        String longName;
        Boolean isTrash;
        List listGroupNames;
        Long idBusinessDictionaryItemTree;
        String dimension;
        String species;

        GoodService goodService = context.getService(GoodService.class);

        def validator = { Fields fields, ValidationResultBuilder builder ->

            Long idGoodFind;
            String groupName ="";

            String trash = "po cięciu"

            idBusinessDictionaryItemTree = window.getFieldsValue().idBusinessDictionaryItemTree;

            String hql ="Select b.nameTree, b.code from BusinessDictionaryItem b where b.idBusinessDictionaryItem =:idBusinessDictionaryItem "
            listGroupNames = dm.executeQuery(hql, ["idBusinessDictionaryItem" : idBusinessDictionaryItemTree]);

            if (listGroupNames.size() > 0) {
                groupName = listGroupNames.get(0)[0];
                groupName = groupName.replaceAll("-", " ");
            }


            longName = groupName;

            dimension = window.getFieldsValue().dimension;

            if (!dimension.equals(null)) {
                longName = longName + " " + dimension.trim();
            }

            species = window.getFieldsValue().species;

            if (!species.equals(null)) {
                longName = longName + " " + species.trim();
            }

            isTrash = window.getFieldsValue().trash;

            if (isTrash) {
                longName = longName + " " + trash.trim();
            }

            idGoodFind = goodService.getIdGoodForFullName(longName);

            if (!idGoodFind.equals(null)) {
                builder.addValidationError("Kartoteka o takiej nazwie już istnieje");
            }
        } as Validator


        window.addValidator(validator);

        if (window.run()) {

            //Generowanie indeksu kartotekowego

            String newIndex = goodService.generateNextIndex(listGroupNames.get(0)[1]);

            String shortName;
            if (longName.length() > 100) {
                shortName = longName.substring(0, 99).trim();
            }
            else{
                shortName = longName.trim();
            }


            GoodDto goodDto = new GoodDto();

            goodDto.goodIndex = newIndex;
            goodDto.shortName = shortName;
            goodDto.longName = longName;
            goodDto.idGoodType = 11L; //Materiał
            goodDto.idWarehouse = 100000L //Magazyn materiałów
            goodDto.goodIsUsedInWarehouse = true;
            goodDto.goodIsUsedInSupply = true;
            goodDto.isPurchaseReverseCharge = window.getFieldsValue().isPurchaseReverseCharge;
            goodDto.isSaleReverseCharge = window.getFieldsValue().isPurchaseReverseCharge;

            goodDto = goodService.init(goodDto);
            goodDto.idUnit = window.getFieldsValue().unitCode;
            goodDto.isIdentifiable = true;
            goodDto.idPartNumberGenStrategy = 100300L// wg. wymiarów
            //  goodDto = goodService.find(idNewGood);

            Long idNewGood = goodService.insert(goodDto);



            //  goodService.update(goodDto);

            //Przypisanie do miejsc
            Long idOrgCompany = 100000L;

            SupplyPlaceService supplyPlaceService = context.getService(SupplyPlaceService.class);
            PurchasePlaceService purchasePlaceService = context.getService(PurchasePlaceService.class);
            List<Long> idsPlaceSupply = supplyPlaceService.getAllSupplyPlaceForCompany(idOrgCompany);
            List<Long> idsPlacePurchase = purchasePlaceService.getAllPurchasePlaceForCompany(idOrgCompany);

            for (Long idPlaceSupply in idsPlaceSupply) {
                goodService.insertSupplyPlaceArticleByGood(idNewGood, idPlaceSupply)
            }
            for (Long idPurchasePlace in idsPlacePurchase) {
                goodService.insertPurchasePlaceArticleForGood(idNewGood, idPurchasePlace);
            }


            goodDto = goodService.find(idNewGood);

            //Przypisanie cech kartoteki

            AttributeValueService attributeValueService = context.getService(AttributeValueService.class);
            Long idAttrGoodSubject = goodDto.idAttributeSubject;
            Long idAttrGoodGroupValue = 100005L; //Grupa materiałowa
            Long idAttrGoodDimensionValue = 100306L //Wymiar
            Long idAttrGoodSpeciesValue = 100300L //Gatunek

            attributeValueService.insertAttributeValueBySubjectAndDef(idAttrGoodSubject, idAttrGoodGroupValue, idBusinessDictionaryItemTree,
                    WhatUseForEditIdBusinessDictionaryItem.USE_NOT_NATIVE_DICTIONARY);
            attributeValueService.insertAttributeValueBySubjectAndDef(idAttrGoodSubject, idAttrGoodDimensionValue, dimension);
            attributeValueService.insertAttributeValueBySubjectAndDef(idAttrGoodSubject, idAttrGoodSpeciesValue, species);

            //Przypisanie cech partii
            def AttributesPartDef;

            if (!isTrash) {
                AttributesPartDef = AttributesPart;
            }
            else{
                AttributesPartDef = AttributesPartNotUseGood;
            }

            PartIdentAttrSubDefService partIdentAttrSubDefService = context.getService(PartIdentAttrSubDefService.class);

            for (attr in AttributesPartDef.values()) {
                PartIdentAttrSubDefDto partDto = new PartIdentAttrSubDefDto();
                partDto.idGood = idNewGood;
                partDto.idAttributeSubjectDef = attr.getIdAttribute();
                partDto.isRequired = false;

                partIdentAttrSubDefService.insert(partDto);
            }

            //Dowiedzieć się jak otworzyć okno z kursorem na nowej pozycji
            // option.openInTab('pl.com.stream.verto.whm.plugin.warehouse-dictionary-client.WarehouseArticleOption', [inIdWarehouseArticle: goodDto.idWarehouseArticle, inIdWarehouse : 100000L]);
        }
    }
}