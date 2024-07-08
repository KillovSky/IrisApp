import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.killovsky.iris.R

class FavoritesDialog(context: Context, private val favoritos: List<String>, private val onFavoriteSelected: (String) -> Unit) : Dialog(context) {

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.favorites_dialog)
        window?.setBackgroundDrawable(ColorDrawable(Color.BLACK))

        val width = (context.resources.displayMetrics.widthPixels * 0.9).toInt()
        window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)

        val recyclerView: RecyclerView = findViewById(R.id.favorites_recycler_view)
        val botaoCancelar: Button = findViewById(R.id.cancel_button)

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = FavoritesAdapter(favoritos, onFavoriteSelected)

        botaoCancelar.setOnClickListener {
            dismiss()
        }
    }
}

class FavoritesAdapter(private val favoritos: List<String>, private val onFavoriteSelected: (String) -> Unit) : RecyclerView.Adapter<FavoritesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val urlFavorita: TextView = view.findViewById(R.id.favorite_url)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_favorite, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val favorito = favoritos[position]
        holder.urlFavorita.text = favorito
        holder.itemView.setOnClickListener {
            onFavoriteSelected(favorito)
        }
    }

    override fun getItemCount(): Int {
        return favoritos.size
    }
}