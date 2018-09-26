/*
classe que gerencia o fragmento do mapa

--  boolean InicializaMapa()

    Gera o fragmento

*/
package br.ind.klein.ClimaGordo;

import android.content.Intent;
import android.location.Location;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


public class MapsActivity extends FragmentActivity implements View.OnClickListener, OnMapReadyCallback{

    private GoogleMap mMap;
    Marker player;

    public boolean modosurfista=false;
    private Location currentlocation;
    private static final float DEFAULTZOOM = 10;

    /************************************************
     *
     * @param savedInstanceState
     *
     * Faz a inicizalização do mapa
     */
	 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        //----------------------------------------------------------

        //Cria o clickListener
        FloatingActionButton buttonGPS = (FloatingActionButton) findViewById(R.id.btnCentraliza);
        FloatingActionButton buttonDetails = (FloatingActionButton) findViewById(R.id.bAccountDetails);
        FloatingActionButton postarClima = (FloatingActionButton) findViewById(R.id.postarClima);
        FloatingActionButton btnModoSurfista = (FloatingActionButton) findViewById(R.id.btnModoSurfista);
        buttonGPS.setOnClickListener(this);
        buttonDetails.setOnClickListener(this);
        postarClima.setOnClickListener(this);
        btnModoSurfista.setOnClickListener(this);

        SupportMapFragment mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if(mapFrag!=null) mapFrag.getMapAsync(this);

    }

    /*************
     *
     * @param googleMap
     *
     * Quando o fragmento do google maps está pronto, chama esta função, que inicizaliza as posicoes e poe o marker do player
     *
     */
	 
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap=googleMap;

        mMap.setInfoWindowAdapter(new MapSnippet(getLayoutInflater()));

        player = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(0,0))
                .title("Você amigo!")
                .snippet("Sim, Você!")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.usercartola))
                .visible(false)
        );

        vaiParaLastKnownLocation(true);

    atualizaclimas();
    }


    @Override
    public void onResume() {
        super.onResume();
        Servicos.pegaServicos().resumeLocationUpdates();
        atualizaclimas();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Servicos.pegaServicos().stopLocationUpdates();
    }


    void atualizaclimas()
    {
        ConexaoJSON conexaoBuscaClimas = new ConexaoJSON();

        conexaoBuscaClimas.tipo=ConexaoJSON.BUSCA_CLIMAS;
        conexaoBuscaClimas.context=this;
        conexaoBuscaClimas.location=Servicos.pegaServicos().getLocation();
        conexaoBuscaClimas.execute();
    }
    /*************************
     *
     * @param v
     *
     * Recebe o click dos botoes
     */
	 
    @Override
    public void onClick(View v) {
        switch(v.getId())
        {
            case R.id.btnCentraliza:
                vaiParaLastKnownLocation(false);

                break;

            case R.id.bAccountDetails:
                startActivity(new Intent(this, AccountSettings.class));
                break;

            case R.id.postarClima:
                startActivity(new Intent(this, SelecionaClima.class));
                break;

            case R.id.btnModoSurfista:
                startActivity(new Intent(this, Tirafoto.class));

                atualizaclimas();
                break;
        }
    }

    /*************
     * Vai para a ultima posicao gps conhecida e move o player também
     */
	 
    public void vaiParaLastKnownLocation(boolean setazoom )
    {
    currentlocation=Servicos.pegaServicos().getLocation();

    Log.d("MAPA","Indo para: "+currentlocation.toString());

    if(currentlocation!=null) {
        player.setPosition(new LatLng(currentlocation.getLatitude(), currentlocation.getLongitude()));
        player.setVisible(true);

        LatLng ll = new LatLng(currentlocation.getLatitude(), currentlocation.getLongitude());
        CameraUpdate update = CameraUpdateFactory.newLatLng(ll);

        if(setazoom==true)
            {
            update=CameraUpdateFactory.newLatLngZoom(ll,DEFAULTZOOM);
            }

        mMap.moveCamera(update);

        }

    }

    public long CalculaTempoPassado(String dateString)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date agora=new Date();
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT-3:00"));

        try {
            long dataagora=agora.getTime()/1000;
            long datapostagem = dateFormat.parse(dateString).getTime()/1000;
            long tempo=dataagora-datapostagem;
            return tempo;
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return 86400;
        }

    }

    public float ConverteSegundosAlpha(long tempo) {
        //passou 1 dias
        if (tempo > 86400) return 0.1f;

        //fazer linear   1-(N/86400+0.1)
        if (tempo <= 0) tempo = 1;
        return 1 - ((((float) tempo) / 96000) + 0.1f);
    }

    /************
     *
     * Retornou os climas do servidor - fazer tudo ainda -
     *
     * @param result
     *
     */
	 
    public void RecebeuClimas(String result)
    {
        LatLng posicaoplayer=player.getPosition();

        boolean visible=player.isVisible();

        mMap.clear();

        try {

            Log.i("BuscaClimas",result);

            JSONObject obj = new JSONObject(result);

            JSONArray arr = obj.getJSONArray("climas");

            for(int i=0;i<arr.length();i++)
            {
                JSONObject clima=arr.getJSONObject(i);


                String data=clima.getString("dt");
                long tempoPassado=CalculaTempoPassado(data);
                float alpha=ConverteSegundosAlpha(tempoPassado);

                clima.put("tempoPassado",tempoPassado);

                String colaborador=clima.getString("col");

                LatLng posicao=new LatLng( clima.getDouble("lt"),clima.getDouble("ln") );
                int meuclima=R.drawable.pinpointpreta;
                int tipoclima=clima.getInt("cl");
                switch(tipoclima)
                {
                    case SelecionaClima.CLIMA_SOL: meuclima=R.drawable.pinpointensolarado;
                        break;
                    case SelecionaClima.CLIMA_ENTRENUVENS: meuclima=R.drawable.pinpointentrenuvens;
                        break;
                    case SelecionaClima.CLIMA_NUBLADO: meuclima=R.drawable.pinpointnublado;
                        break;
                    case SelecionaClima.CLIMA_RAIO: meuclima=R.drawable.pinpointtemporal;
                        break;
                    case SelecionaClima.CLIMA_CHUVA: meuclima=R.drawable.pinpointchuva;
                        break;
                    case SelecionaClima.CLIMA_VENDAVAL: meuclima=R.drawable.pinpointvendaval;
                        break;
                    case SelecionaClima.CLIMA_GRANIZO: meuclima=R.drawable.pinpointgranizo;
                        break;
                    case SelecionaClima.CLIMA_NEVE: meuclima=R.drawable.pinpointneve;
                        break;
                    case SelecionaClima.CLIMA_NEBLINA: meuclima=R.drawable.pinpointneblina;
                        break;
                    case SelecionaClima.CLIMA_ARCOIRIS: meuclima=R.drawable.pinpointarcoiris;
                        break;
                    case SelecionaClima.CLIMA_UFO: meuclima=R.drawable.pinpoinufo;
                        break;
                    case SelecionaClima.CLIMA_ESTRELADO: meuclima=R.drawable.pinpointestrelado;
                        break;
                    case SelecionaClima.CLIMA_ESTRELADOCOMSNUVENS: meuclima=R.drawable.usercartola;
                        break;

                    case 666: meuclima=R.drawable.pinpointpreta;
                        break;

                    /*case SelecionaClima.CLIMA_BRANCA: meuclima=R.drawable.pinpointbranca;
                        break;
                    case SelecionaClima.CLIMA_AMARELA: meuclima=R.drawable.pinpointamarela;
                        break;
                    case SelecionaClima.CLIMA_VERMELHA: meuclima=R.drawable.pinpointvermelha;
                        break;
                     */

                }
                //setInfoWindow(mapsnippet)
                Marker novo=mMap.addMarker(new MarkerOptions()
                        .position(posicao)
                        //TODO: testar usuario do cara do backend
                        .title(colaborador)
                        .snippet(clima.toString()) //
                        .alpha(alpha)
                        .icon(BitmapDescriptorFactory.fromResource(meuclima))
                        .zIndex(i).infoWindowAnchor(0.5f,0.5f)
                );

            }

/*      //Ainda não sabemos se vamos usar o marker do usuário

            player = mMap.addMarker(new MarkerOptions()
                    .position(posicaoplayer)
                    //.zIndex(1.0f)
                    .title("Você amigo!")
                    .snippet("Sim, Você!")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.usercartola))
                    .visible(visible)
            );

*/
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //Do something with the JSON string
    }






}

